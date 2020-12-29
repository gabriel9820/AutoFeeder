package com.android.gabriel.autofeeder.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.api.UsuarioApi;
import com.android.gabriel.autofeeder.model.Usuario;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.android.gabriel.autofeeder.utils.Util.isEmailInvalido;

public class EsqueceuSenhaActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnConfirmar;

    private Retrofit mRetrofit;
    private UsuarioApi mUsuarioApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esqueceu_senha);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.redefinicao_senha));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //mostra o botão voltar
        getSupportActionBar().setHomeButtonEnabled(true);      //ativa o botão voltar

        etEmail = findViewById(R.id.etEmail);
        btnConfirmar = findViewById(R.id.btnConfirmar);

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mUsuarioApi = mRetrofit.create(UsuarioApi.class);

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validaCampos()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(EsqueceuSenhaActivity.this);

                    final Usuario usuario = new Usuario();
                    usuario.setEmail(etEmail.getText().toString());

                    Call<Void> call = mUsuarioApi.forgotPassword(usuario);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                builder.setTitle(R.string.titulo_mensagem);
                                builder.setMessage(getString(R.string.email_redefinicao_senha, usuario.getEmail()));
                                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onBackPressed();
                                    }
                                });
                            }
                            else {
                                builder.setMessage(getString(R.string.erro) + " " + response.code() + " " + response.message());
                            }

                            builder.create();
                            builder.show();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(EsqueceuSenhaActivity.this, getString(R.string.sem_conexao_api), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean validaCampos() {
        if (isEmailInvalido(String.valueOf(etEmail.getText()))) {
            Toast.makeText(EsqueceuSenhaActivity.this, getString(R.string.campo_invalido), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        return true;
    }
}
