package com.android.gabriel.autofeeder.ui.refeicoes_programadas;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.api.RefeicaoApi;
import com.android.gabriel.autofeeder.model.Refeicao;
import com.android.gabriel.autofeeder.model.UsuarioLogado;
import com.android.gabriel.autofeeder.services.ConexaoBluetooth;
import com.android.gabriel.autofeeder.utils.Util;

import java.io.Serializable;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RefeicoesProgramadasFragment extends Fragment {

    private RefeicoesProgramadasViewModel refeicoesProgramadasViewModel;

    private ListView lvRefeicoes;
    ArrayAdapter<Refeicao> mRefeicoesAdapter;
    private Button btnZerarRefeicoes;

    private List<Refeicao> mRefeicoes;
    private UsuarioLogado mUsuarioLogado;
    private RefeicaoApi mRefeicaoApi;
    private Retrofit mRetrofit;
    private ConexaoBluetooth mConexaoBluetooth;
    private StringBuilder DataStringIN = new StringBuilder();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        refeicoesProgramadasViewModel =
                ViewModelProviders.of(this).get(RefeicoesProgramadasViewModel.class);
        View root = inflater.inflate(R.layout.fragment_refeicoes_programadas, container, false);

        lvRefeicoes = root.findViewById(R.id.lvRefeicoes);
        lvRefeicoes.setOnItemClickListener(lvRefeicoesClickListener);
        btnZerarRefeicoes = root.findViewById(R.id.btnZerarRefeicoes);

        mUsuarioLogado = UsuarioLogado.getInstance();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mRefeicaoApi = mRetrofit.create(RefeicaoApi.class);

        listarRefeicoes();

        mConexaoBluetooth = ConexaoBluetooth.getInstance();
        mConexaoBluetooth.setHandler(mHandler);

        btnZerarRefeicoes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConexaoBluetooth.bluetoothConectado()) {
                    mConexaoBluetooth.mConnectedThread.write(Util.LIMPAR_EEPROM + Util.SEPARADOR);
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

    private AdapterView.OnItemClickListener lvRefeicoesClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
            Refeicao refeicao = mRefeicoesAdapter.getItem(pos);

            Bundle bundle = new Bundle();
            bundle.putSerializable("refeicao", refeicao);
            bundle.putSerializable("refeicoes", (Serializable) mRefeicoes);

            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_cadastro_refeicao, bundle);
        }
    };

    private void listarRefeicoes() {
        Call<List<Refeicao>> call = mRefeicaoApi.getAll("Bearer " + mUsuarioLogado.getAccessToken(), mUsuarioLogado.getId());

        call.enqueue(new Callback<List<Refeicao>>() {
            @Override
            public void onResponse(Call<List<Refeicao>> call, Response<List<Refeicao>> response) {
                if (response.isSuccessful()) {
                    mRefeicoes = response.body();
                    mRefeicoesAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, mRefeicoes);
                    lvRefeicoes.setAdapter(mRefeicoesAdapter);
                } else {
                    Toast.makeText(getContext(), getString(R.string.erro) + " " + response.code() + " " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Refeicao>> call, Throwable t) {
                Toast.makeText(getContext(), getString(R.string.sem_conexao_api), Toast.LENGTH_LONG).show();
            }
        });
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH:
                    Intent habilitarBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(habilitarBluetooth, ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH);
                    break;
                case ConexaoBluetooth.ERRO_CONEXAO:
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
                            case Util.LIMPAR_EEPROM:
                                Toast.makeText(getContext(), getString(R.string.ok), Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };
}