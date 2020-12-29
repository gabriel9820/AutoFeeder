package com.android.gabriel.autofeeder.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ConexaoBluetooth {
    private static ConexaoBluetooth sInstancia;

    private Handler mHandler;
    private BluetoothAdapter mBTAdapter = null;
    private BluetoothSocket mBTSocket = null;
    public ConnectedThread mConnectedThread;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //tipo de mensagem enviada ao handler
    public static final int REQUISICAO_HABILITAR_BLUETOOTH = 0;
    public static final int ERRO_CONEXAO = 1;
    public static final int STATUS_ADAPTADOR = 2;
    public static final int STATUS_CONEXAO = 3;
    public static final int DADOS_RECEBIDOS = 4;

    private ConexaoBluetooth() { }

    public static ConexaoBluetooth getInstance() {
        if(sInstancia == null) {
            sInstancia = new ConexaoBluetooth();
        }

        return sInstancia;
    }

    public void conectar(String address) {
        if (bluetoothHabilitado()) {
            BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

            //cria o socket
            try {
                mBTSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                mHandler.obtainMessage(STATUS_CONEXAO, -1, -1, "A criação do Socket falhou").sendToTarget();
            }

            //tenta conectar
            try {
                mBTSocket.connect();
                mHandler.obtainMessage(STATUS_CONEXAO, -1, -1, "Conectado com sucesso").sendToTarget();
            } catch (IOException e) {
                try {
                    mBTSocket.close();
                } catch (IOException e2) { }
            }

            mConnectedThread = new ConnectedThread(mBTSocket);
            mConnectedThread.start();
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //cria uma conexão de saída segura usando o serviço UUID (sem interferência de outros dispositivos bluetooth)
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    //verifica se o bluetooth do celular está habilitado
    public boolean bluetoothHabilitado() {
        if (mBTAdapter == null) {
            mHandler.obtainMessage(STATUS_ADAPTADOR, -1, -1, "O dispositivo não possui bluetooth!").sendToTarget();
            return false;
        } else if (!mBTAdapter.isEnabled()) {
            mHandler.obtainMessage(REQUISICAO_HABILITAR_BLUETOOTH, -1, -1, -1).sendToTarget();
        }

        return mBTAdapter.isEnabled();
    }

    public boolean bluetoothConectado() {
        if (mBTAdapter == null) {
            return false;
        } else if (!mBTAdapter.isEnabled()) {
            return false;
        } else if (mBTSocket == null) {
            return false;
        }

        return mBTSocket.isConnected();
    }

    public Set<BluetoothDevice> getDispositivosPareados() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        return mBTAdapter.getBondedDevices();
    }

    //classe que faz a comunicação bluetooth
    public class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            //loop para se manter em modo de escuta
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    //envia os dados recebidos para a tela via Handler
                    mHandler.obtainMessage(DADOS_RECEBIDOS, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        //envia os dados ao Arduino
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                //se der erro fecha a conexão automaticamente
                mHandler.obtainMessage(ERRO_CONEXAO, -1, -1, "Erro de comunicação bluetooth").sendToTarget();
                close();
            }
        }

        //fecha a conexão bluetooth
        public void close() {
            try {
                mBTSocket.close();
            } catch (IOException e) { }
        }
    }
}