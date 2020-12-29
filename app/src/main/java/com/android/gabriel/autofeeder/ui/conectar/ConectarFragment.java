package com.android.gabriel.autofeeder.ui.conectar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;
import android.widget.Toast;

import com.android.gabriel.autofeeder.services.ConexaoBluetooth;
import com.android.gabriel.autofeeder.R;

import java.util.Set;

import static android.app.Activity.RESULT_OK;

public class ConectarFragment extends Fragment {

    private ConectarViewModel conectarViewModel;

    private ListView lvPareados;
    private ArrayAdapter<String> mPareadosAdapter;

    private ConexaoBluetooth mConexaoBluetooth;
    private StringBuilder DataStringIN = new StringBuilder();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        conectarViewModel =
                ViewModelProviders.of(this).get(ConectarViewModel.class);
        View root = inflater.inflate(R.layout.fragment_conectar, container, false);

        lvPareados = root.findViewById(R.id.lvPareados);

        mPareadosAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        lvPareados.setAdapter(mPareadosAdapter);
        lvPareados.setOnItemClickListener(pareadosClickListener);

        mConexaoBluetooth = ConexaoBluetooth.getInstance();
        mConexaoBluetooth.setHandler(mHandler);

        listarDispositivosPareados();

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ConexaoBluetooth.REQUISICAO_HABILITAR_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                listarDispositivosPareados();
            }
        }
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
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    private void listarDispositivosPareados() {
        Set<BluetoothDevice> dispositivosPareados;

        mPareadosAdapter.clear();
        dispositivosPareados = mConexaoBluetooth.getDispositivosPareados();

        if (mConexaoBluetooth.bluetoothHabilitado()) {
            for (BluetoothDevice device : dispositivosPareados) {
                mPareadosAdapter.add(device.getName() + System.getProperty("line.separator") + device.getAddress());
            }
        }
    }

    private AdapterView.OnItemClickListener pareadosClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            //gera uma nova thread para não travar a tela enquanto tenta conectar
            new Thread()
            {
                public void run() {
                    mConexaoBluetooth.conectar(address);
                }
            }.start();
        }
    };
}