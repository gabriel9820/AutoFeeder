package com.android.gabriel.autofeeder.ui.inicio;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;
import android.widget.Toast;

import com.android.gabriel.autofeeder.services.ConexaoBluetooth;
import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.utils.Util;
import com.android.gabriel.autofeeder.api.RefeicaoApi;
import com.android.gabriel.autofeeder.model.Refeicao;
import com.android.gabriel.autofeeder.model.UsuarioLogado;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InicioFragment extends Fragment {

    private InicioViewModel inicioViewModel;

    private TextView tvDataAtual;
    private TextView tvNumeroRefeicao;
    private TextView tvHoraRefeicao;
    private TextView tvQuantidadeRefeicao;
    private TextView tvSensorPote;
    private ImageView ivAtualizar;
    private ImageView ivCirculoStatus;
    private TextView tvStatus;

    private UsuarioLogado mUsuarioLogado;
    private RefeicaoApi mRefeicaoApi;
    private Retrofit mRetrofit;
    private ConexaoBluetooth mConexaoBluetooth;
    private StringBuilder DataStringIN = new StringBuilder();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        inicioViewModel =
                ViewModelProviders.of(this).get(InicioViewModel.class);
        View root = inflater.inflate(R.layout.fragment_inicio, container, false);

        tvDataAtual = root.findViewById(R.id.tvDataAtual);
        tvNumeroRefeicao = root.findViewById(R.id.tvNumeroRefeicao);
        tvHoraRefeicao = root.findViewById(R.id.tvHoraRefeicao);
        tvQuantidadeRefeicao = root.findViewById(R.id.tvQuantidadeRefeicao);
        tvSensorPote = root.findViewById(R.id.tvSensorPote);
        ivAtualizar = root.findViewById(R.id.ivAtualizar);
        ivCirculoStatus = root.findViewById(R.id.ivCirculoStatus);
        tvStatus = root.findViewById(R.id.tvStatus);

        mUsuarioLogado = UsuarioLogado.getInstance();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mRefeicaoApi = mRetrofit.create(RefeicaoApi.class);

        mConexaoBluetooth = ConexaoBluetooth.getInstance();
        mConexaoBluetooth.setHandler(mHandler);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy");
        Date hoje = Calendar.getInstance(Locale.getDefault()).getTime();
        tvDataAtual.setText(sdf.format(hoje));

        ivAtualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConexaoBluetooth.bluetoothConectado()) {
                    mConexaoBluetooth.mConnectedThread.write(Util.CONSULTAR_SENSOR_POTE + Util.SEPARADOR);
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

    @Override
    public void onResume() {
        super.onResume();

        proximaRefeicao();
        mostrarConexao(mConexaoBluetooth.bluetoothConectado());
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH:
                    Intent habilitarBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(habilitarBluetooth, ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH);
                    break;
                case ConexaoBluetooth.ERRO_CONEXAO:
                    String erro = (String) msg.obj;
                    Toast.makeText(getContext(), erro, Toast.LENGTH_LONG).show();
                    mostrarConexao(false);
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
                            case Util.CONSULTAR_SENSOR_POTE:
                                tvSensorPote.setText("Pote: " + dados[1] + " g");
                                break;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    private void proximaRefeicao() {
        Call<Refeicao> call = mRefeicaoApi.getProximaRefeicao("Bearer " + mUsuarioLogado.getAccessToken(), mUsuarioLogado.getId());

        call.enqueue(new Callback<Refeicao>() {
            @Override
            public void onResponse(Call<Refeicao> call, Response<Refeicao> response) {
                if (response.isSuccessful()) {
                    tvNumeroRefeicao.setText(String.format("%d/5", response.body().getNumero()));
                    tvHoraRefeicao.setText(response.body().getHorario().substring(0, 5));
                    tvQuantidadeRefeicao.setText(String.format("%d g", response.body().getQuantidade()));
                } else {
                    if (response.code() != 404) {
                        Toast.makeText(getContext(), getString(R.string.erro) + " " + response.code() + " " + response.message(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Refeicao> call, Throwable t) {
                Toast.makeText(getContext(), t.getCause().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarConexao(boolean conectado) {
        if (conectado) {
            ivCirculoStatus.setImageResource(R.drawable.circulo_verde);
            tvStatus.setText(R.string.conectado);

            mConexaoBluetooth.mConnectedThread.write(Util.CONSULTAR_SENSOR_POTE + Util.SEPARADOR);
        } else {
            ivCirculoStatus.setImageResource(R.drawable.circulo_vermelho);
            tvStatus.setText(R.string.desconectado);

            tvSensorPote.setText("Pote: 0 g");
        }
    }
}