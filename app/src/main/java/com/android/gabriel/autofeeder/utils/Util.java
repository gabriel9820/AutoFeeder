package com.android.gabriel.autofeeder.utils;

import android.util.Patterns;

public class Util {
    public static final String SEPARADOR = System.getProperty("line.separator");
    public static final int CONSULTAR_SENSOR_POTE = 1;
    public static final int CONSULTAR_HORA = 3;
    public static final int AJUSTAR_HORA = 4;
    public static final int REFEICAO_MANUAL = 5;
    public static final int GRAVAR_REFEICAO_1 = 6;
    public static final int GRAVAR_REFEICAO_2 = 7;
    public static final int GRAVAR_REFEICAO_3 = 8;
    public static final int GRAVAR_REFEICAO_4 = 9;
    public static final int GRAVAR_REFEICAO_5 = 10;
    public static final int LIMPAR_EEPROM = 999;

    public static boolean isCampoVazio(String texto) {
        return texto.trim().isEmpty();
    }

    public static boolean isCelularInvalido(String celular) {
        return !celular.matches("(\\(\\d{2}\\)\\s)(\\d{5}\\-\\d{4})");
    }

    public static boolean isEmailInvalido(String email) {
        return (isCampoVazio(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

    public static boolean isCamposIguais(String... strings) {
        return strings[0].equals(strings[1]);
    }
}
