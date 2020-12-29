package com.android.gabriel.autofeeder.utils;

import java.util.Timer;
import java.util.TimerTask;

public class Relogio {
    private int hora;
    private int minuto;
    private int segundo;
    private Timer timer;

    public Relogio(int hora, int minuto, int segundo) {
        this.hora = hora;
        this.minuto = minuto;
        this.segundo = segundo;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pulso();
            }
        }, 0, 1000);
    }

    public String getHora() {
        return String.format("%02d", hora) + ":" + String.format("%02d", minuto) + ":" + String.format("%02d", segundo);
    }

    public String enviarHoraArduino() {
        return hora + ";" + minuto + ";" + segundo;
    }

    private void pulso() {
        segundo++;

        if (segundo > 59) {
            segundo = 0;
            minuto++;
        }
        if (minuto > 59) {
            minuto = 0;
            hora++;
        }
        if (hora > 23) {
            hora = 0;
        }
    }
}
