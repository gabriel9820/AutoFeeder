package com.android.gabriel.autofeeder.ui.ajustar_hora;

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
import android.widget.TextView;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;
import android.widget.Toast;

import com.android.gabriel.autofeeder.services.ConexaoBluetooth;
import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.utils.Relogio;
import com.android.gabriel.autofeeder.utils.Util;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class AjustarHoraFragment extends Fragment {

    private AjustarHoraViewModel ajustarHoraViewModel;

    private TextView tvHoraCelular;
    private TextView tvHoraAlimentador;
    private Button btnConfirmar;

    private Relogio mRelogioCelular;
    private Relogio mRelogioAlimentador;
    private Timer mTimer;
    private ConexaoBluetooth mConexaoBluetooth;
    private StringBuilder DataStringIN = new StringBuilder();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ajustarHoraViewModel =
                ViewModelProviders.of(this).get(AjustarHoraViewModel.class);
        View root = inflater.inflate(R.layout.fragment_ajustar_hora, container, false);

        tvHoraCelular = root.findViewById(R.id.tvHoraCelular);
        tvHoraAlimentador = root.findViewById(R.id.tvHoraAlimentador);
        btnConfirmar = root.findViewById(R.id.btnConfirmar);

        mConexaoBluetooth = ConexaoBluetooth.getInstance();
        mConexaoBluetooth.setHandler(mHandler);
        mTimer = new Timer();

        if (mConexaoBluetooth.bluetoothConectado()) {
            mConexaoBluetooth.mConnectedThread.write(Util.CONSULTAR_HORA + Util.SEPARADOR);
        }

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConexaoBluetooth.bluetoothConectado()) {
                    mConexaoBluetooth.mConnectedThread.write(Util.AJUSTAR_HORA + ";" + mRelogioCelular.enviarHoraArduino() + Util.SEPARADOR);
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
    public void onDestroy() {
        super.onDestroy();

        mTimer.cancel();
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
                            case Util.CONSULTAR_HORA:
                                inicilizarRelogios(Integer.parseInt(dados[1]), Integer.parseInt(dados[2]), Integer.parseInt(dados[3]));
                                break;
                            case Util.AJUSTAR_HORA:
                                inicilizarRelogios(Integer.parseInt(dados[1]), Integer.parseInt(dados[2]), Integer.parseInt(dados[3]));
                                break;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    private void inicilizarRelogios(int hora, int minuto, int segundo) {
        Calendar calendar = Calendar.getInstance();
        mRelogioCelular = new Relogio(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
        mRelogioAlimentador = new Relogio(hora, minuto, segundo);

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvHoraCelular.setText(mRelogioCelular.getHora());
                        tvHoraAlimentador.setText(mRelogioAlimentador.getHora());
                    }
                });
            }
        }, 0, 1000);
    }
}