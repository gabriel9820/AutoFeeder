package com.android.gabriel.autofeeder.ui.cadastro_refeicao;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.gabriel.autofeeder.services.ConexaoBluetooth;
import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.utils.Util;
import com.android.gabriel.autofeeder.api.RefeicaoApi;
import com.android.gabriel.autofeeder.model.Refeicao;
import com.android.gabriel.autofeeder.model.UsuarioLogado;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.android.gabriel.autofeeder.utils.Util.isCampoVazio;

public class CadastroRefeicaoFragment extends Fragment {

    private CadastroRefeicaoViewModel cadastroRefeicaoViewModel;

    private TimePicker tpRefeicao;
    private EditText etQuantidade;
    private Switch switchAtivo;
    private Button btnConfirmar;

    private Refeicao mRefeicaoOriginal;
    private List<Refeicao> mRefeicoes;
    private RefeicaoApi mRefeicaoApi;
    private Retrofit mRetrofit;
    private UsuarioLogado mUsuarioLogado;
    private ConexaoBluetooth mConexaoBluetooth;
    private StringBuilder DataStringIN = new StringBuilder();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cadastroRefeicaoViewModel =
                ViewModelProviders.of(this).get(CadastroRefeicaoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cadastro_refeicao, container, false);

        tpRefeicao = root.findViewById(R.id.tpRefeicao);
        etQuantidade = root.findViewById(R.id.etQuantidade);
        switchAtivo = root.findViewById(R.id.switchAtivo);
        btnConfirmar = root.findViewById(R.id.btnConfirmar);
        tpRefeicao.setIs24HourView(true);

        mRefeicaoOriginal = (Refeicao) getArguments().getSerializable("refeicao");
        mRefeicoes = (List<Refeicao>) getArguments().getSerializable("refeicoes");
        preencherCampos();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mRefeicaoApi = mRetrofit.create(RefeicaoApi.class);

        mUsuarioLogado = mUsuarioLogado.getInstance();

        mConexaoBluetooth = ConexaoBluetooth.getInstance();
        mConexaoBluetooth.setHandler(mHandler);

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConexaoBluetooth.bluetoothConectado()) {
                    if (validarCampos()) {
                        Refeicao refeicao = new Refeicao();
                        refeicao.setId(mRefeicaoOriginal.getId());
                        refeicao.setUserId(mUsuarioLogado.getId());
                        refeicao.setNumero(mRefeicaoOriginal.getNumero());
                        if (Build.VERSION.SDK_INT >= 23) {
                            refeicao.setHorario(tpRefeicao.getHour() + ":" + tpRefeicao.getMinute() + ":00");
                        } else {
                            refeicao.setHorario(tpRefeicao.getCurrentHour() + ":" + tpRefeicao.getCurrentMinute() + ":00");
                        }
                        refeicao.setQuantidade(Integer.parseInt(etQuantidade.getText().toString()));
                        refeicao.setAtivo(switchAtivo.isChecked());

                        if (validarHorario(refeicao)) {
                            salvarRefeicaoBD(refeicao, true);
                        }
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.titulo_mensagem);
                    builder.setMessage(getString(R.string.bluetooh_desconectado));
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.create();
                    builder.show();
                }
            }
        });

        return root;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH:
                    Intent habilitarBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(habilitarBluetooth, ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH);
                    break;
                case ConexaoBluetooth.ERRO_CONEXAO:
                    //se der erro ao salvar no arduino devolve os valores originais ao BD
                    salvarRefeicaoBD(mRefeicaoOriginal, false);
                    preencherCampos();
                    String erro = (String) msg.obj;
                    Toast.makeText(getContext(), erro, Toast.LENGTH_LONG).show();
                    break;
                case ConexaoBluetooth.STATUS_ADAPTADOR:
                case ConexaoBluetooth.STATUS_CONEXAO:
                    String mensagem = (String) msg.obj;
                    Toast.makeText(getContext(), mensagem, Toast.LENGTH_LONG).show();
                    break;
                case ConexaoBluetooth.DADOS_RECEBIDOS:
                    String strDados = (String) msg.obj;
                    DataStringIN.append(strDados);

                    int endOfLineIndex = DataStringIN.indexOf(System.getProperty("line.separator"));

                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        DataStringIN.delete(0, DataStringIN.length());

                        String[] dados = dataInPrint.split(";");
                        int comando = Integer.parseInt(dados[0]);

                        switch (comando) {
                            case Util.GRAVAR_REFEICAO_1:
                            case Util.GRAVAR_REFEICAO_2:
                            case Util.GRAVAR_REFEICAO_3:
                            case Util.GRAVAR_REFEICAO_4:
                            case Util.GRAVAR_REFEICAO_5:
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle(R.string.titulo_mensagem);
                                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                                builder.setMessage(getString(R.string.salvo));
                                builder.create();
                                builder.show();
                                break;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    private void salvarRefeicaoBD(Refeicao refeicao, final boolean enviarArduino) {
        Call<Refeicao> call = mRefeicaoApi.put("Bearer " + mUsuarioLogado.getAccessToken(), refeicao.getId(), refeicao);

        call.enqueue(new Callback<Refeicao>() {
            @Override
            public void onResponse(Call<Refeicao> call, Response<Refeicao> response) {
                if (response.isSuccessful()) {
                    //se salvou no BD envia para o arduino
                    if (enviarArduino) {
                        int ativo = switchAtivo.isChecked() ? 1 : 0;
                        int gravarRefeicao = 0;

                        switch (mRefeicaoOriginal.getNumero()) {
                            case 1:
                                gravarRefeicao = Util.GRAVAR_REFEICAO_1;
                                break;
                            case 2:
                                gravarRefeicao = Util.GRAVAR_REFEICAO_2;
                                break;
                            case 3:
                                gravarRefeicao = Util.GRAVAR_REFEICAO_3;
                                break;
                            case 4:
                                gravarRefeicao = Util.GRAVAR_REFEICAO_4;
                                break;
                            case 5:
                                gravarRefeicao = Util.GRAVAR_REFEICAO_5;
                                break;
                        }

                        if (Build.VERSION.SDK_INT >= 23) {
                            mConexaoBluetooth.mConnectedThread.write(
                                    gravarRefeicao + ";" +
                                            tpRefeicao.getHour() + ";" +
                                            tpRefeicao.getMinute() + ";" +
                                            etQuantidade.getText().toString() + ";" +
                                            ativo +
                                            Util.SEPARADOR);
                        } else {
                            mConexaoBluetooth.mConnectedThread.write(
                                    gravarRefeicao + ";" +
                                            tpRefeicao.getCurrentHour() + ";" +
                                            tpRefeicao.getCurrentMinute() + ";" +
                                            etQuantidade.getText().toString() + ";" +
                                            ativo +
                                            Util.SEPARADOR);
                        }
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                    builder.setTitle(R.string.titulo_mensagem);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.setMessage(getString(R.string.erro) + " " + response.code() + " " + response.message());
                    builder.create();
                    builder.show();
                }
            }

            @Override
            public void onFailure(Call<Refeicao> call, Throwable t) {
                Toast.makeText(getContext(), getString(R.string.sem_conexao_api), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validarCampos() {
        if (isCampoVazio(String.valueOf(etQuantidade.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etQuantidade.requestFocus();
            return false;
        } else {
            int quantidade = Integer.parseInt(String.valueOf(etQuantidade.getText()));

            if (quantidade < 10 || quantidade > 100) {
                Toast.makeText(getContext(), getString(R.string.quantidade_invalida), Toast.LENGTH_SHORT).show();
                etQuantidade.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void preencherCampos() {
        if (Build.VERSION.SDK_INT >= 23) {
            tpRefeicao.setHour(Integer.parseInt(mRefeicaoOriginal.getHorario().substring(0,2)));
            tpRefeicao.setMinute(Integer.parseInt(mRefeicaoOriginal.getHorario().substring(3,5)));
        } else {
            tpRefeicao.setCurrentHour(Integer.parseInt(mRefeicaoOriginal.getHorario().substring(0,2)));
            tpRefeicao.setCurrentMinute(Integer.parseInt(mRefeicaoOriginal.getHorario().substring(3,5)));
        }

        etQuantidade.setText(String.valueOf(mRefeicaoOriginal.getQuantidade()));
        switchAtivo.setChecked(mRefeicaoOriginal.isAtivo());
    }

    private boolean validarHorario(Refeicao refeicaoAtual) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        long msRefeicaoAtual = 0;

        try {
            msRefeicaoAtual = sdf.parse(refeicaoAtual.getHorario()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        for (Refeicao refeicao: mRefeicoes) {
            if (refeicao.getNumero() < refeicaoAtual.getNumero()) {
                long msRefeicaoComparada = 0;

                try {
                    msRefeicaoComparada = sdf.parse(refeicao.getHorario()).getTime() + 240000; //240000 = 4 minutos convertidos em milissegundos
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                //não permite que o horário da refeição atual tenha um intervalo inferior a 5 minutos em relação com a anterior
                if (msRefeicaoAtual <= msRefeicaoComparada) {
                    long msHorarioMinimo = msRefeicaoComparada + 60000; //60000 = 1 minuto convertido em milissegundos (os outros quatro minutos ja haviam sido somados antes do if)
                    Date date = new Date(msHorarioMinimo);
                    String horarioMinimo = sdf.format(date);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.titulo_mensagem);
                    builder.setMessage(getString(R.string.horario_invalido) +
                            System.getProperty("line.separator") +
                            System.getProperty("line.separator") +
                            "Horário anterior: " + refeicao.getHorario() +
                            System.getProperty("line.separator") +
                            "Horário mínimo: " + horarioMinimo);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.create();
                    builder.show();

                    return false;
                }

                //não permite pular refeições
                if (refeicaoAtual.isAtivo()) {
                    if (!refeicao.isAtivo()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(R.string.titulo_mensagem);
                        builder.setMessage(getString(R.string.refeicao_invalida) +
                                System.getProperty("line.separator") +
                                System.getProperty("line.separator") +
                                "Refeição: " + refeicao.getNumero());
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        builder.create();
                        builder.show();

                        return false;
                    }
                }
            }
        }

        return true;
    }
}