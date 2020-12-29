package com.android.gabriel.autofeeder.ui.refeicao_manual;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;
import android.widget.Toast;

import com.android.gabriel.autofeeder.services.ConexaoBluetooth;
import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.utils.Util;

import static com.android.gabriel.autofeeder.utils.Util.*;

public class RefeicaoManualFragment extends Fragment {

    private RefeicaoManualViewModel refeicaoManualViewModel;

    private EditText etQuantidade;
    private Button btnConfirmar;

    private ConexaoBluetooth mConexaoBluetooth;
    private StringBuilder DataStringIN = new StringBuilder();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        refeicaoManualViewModel =
                ViewModelProviders.of(this).get(RefeicaoManualViewModel.class);
        View root = inflater.inflate(R.layout.fragment_refeicao_manual, container, false);

        etQuantidade = root.findViewById(R.id.etQuantidade);
        btnConfirmar = root.findViewById(R.id.btnConfirmar);

        mConexaoBluetooth = ConexaoBluetooth.getInstance();
        mConexaoBluetooth.setHandler(mHandler);

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConexaoBluetooth.bluetoothConectado()) {
                    if (validaCampos()) {
                        mConexaoBluetooth.mConnectedThread.write(Util.REFEICAO_MANUAL + ";" + etQuantidade.getText().toString() + Util.SEPARADOR);
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
                case ConexaoBluetooth.STATUS_ADAPTADOR:
                case ConexaoBluetooth.STATUS_CONEXAO:
                    String mensagem = (String) msg.obj;
                    Toast.makeText(getContext(), mensagem, Toast.LENGTH_LONG).show();
                    break;
                case ConexaoBluetooth.DADOS_RECEBIDOS :
                    String strDados = (String) msg.obj;
                    DataStringIN.append(strDados);

                    int endOfLineIndex = DataStringIN.indexOf(System.getProperty("line.separator"));

                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        DataStringIN.delete(0, DataStringIN.length());

                        String[] dados = dataInPrint.split(";");
                        int comando = Integer.parseInt(dados[0]);

                        switch (comando) {
                            case Util.REFEICAO_MANUAL:
                                //dados[1] = tempoRefeicao (se for == 0 não passou pelo while que gira o motor)
                                if (Integer.parseInt(dados[1]) == 0) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle(R.string.titulo_mensagem);
                                    builder.setMessage(getString(R.string.quantidade_insuficiente) +
                                            System.getProperty("line.separator") +
                                            System.getProperty("line.separator") +
                                            getString(R.string.quantidade_pote, dados[2]));
                                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                                    builder.create();
                                    builder.show();
                                }
                                break;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    private boolean validaCampos() {
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
}