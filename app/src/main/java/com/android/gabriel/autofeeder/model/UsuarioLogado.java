package com.android.gabriel.autofeeder.model;

public class UsuarioLogado extends Usuario {
    private static UsuarioLogado sInstancia;

    private UsuarioLogado () { }

    public static UsuarioLogado getInstance() {
        if (sInstancia == null) {
            sInstancia = new UsuarioLogado();
        }

        return sInstancia;
    }
}
