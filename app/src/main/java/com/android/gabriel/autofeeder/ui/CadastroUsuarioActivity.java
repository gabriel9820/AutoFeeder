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

import com.android.gabriel.autofeeder.utils.Mask;
import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.api.UsuarioApi;
import com.android.gabriel.autofeeder.model.Usuario;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.android.gabriel.autofeeder.utils.Util.*;

public class CadastroUsuarioActivity extends AppCompatActivity {

    private EditText etNome;
    private EditText etCelular;
    private EditText etEmail;
    private EditText etSenha;
    private EditText etRepetirSenha;
    private Button btnConfirmar;

    private UsuarioApi mUsuarioApi;
    private Retrofit mRetrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_usuario);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.criar_conta));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //mostra o botão voltar
        getSupportActionBar().setHomeButtonEnabled(true);      //ativa o botão voltar

        etNome = findViewById(R.id.etNome);
        etCelular = findViewById(R.id.etCelular);
        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        etRepetirSenha = findViewById(R.id.etRepetirSenha);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        etCelular.addTextChangedListener(Mask.insert(Mask.CELULAR_MASK, etCelular));

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mUsuarioApi = mRetrofit.create(UsuarioApi.class);

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validaCampos()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(CadastroUsuarioActivity.this);

                    final Usuario usuario = new Usuario();
                    usuario.setFullName(etNome.getText().toString());
                    usuario.setPhoneNumber(etCelular.getText().toString());
                    usuario.setEmail(etEmail.getText().toString());
                    usuario.setPassword(etSenha.getText().toString());
                    usuario.setConfirmPassword(etRepetirSenha.getText().toString());

                    Call<Void> call = mUsuarioApi.register(usuario);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                builder.setTitle(R.string.titulo_mensagem);
                                builder.setMessage(
                                        getString(R.string.cadastro_sucesso) +
                                        System.getProperty("line.separator") +
                                        System.getProperty("line.separator") +
                                        getString(R.string.email_confirmacao, usuario.getEmail()));
                                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onBackPressed();
                                    }
                                });
                            }
                            else {
                                try {
                                    JSONObject jObjErro = new JSONObject(response.errorBody().string());
                                    builder.setTitle(R.string.titulo_mensagem);
                                    builder.setMessage(jObjErro.getString("ModelState"));
                                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                                } catch (Exception e) {
                                    Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.erro) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

                            builder.create();
                            builder.show();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.sem_conexao_api), Toast.LENGTH_LONG).show();
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
        if (isCampoVazio(String.valueOf(etNome.getText()))) {
            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etNome.requestFocus();
            return false;
        }
        else if (isCelularInvalido(String.valueOf(etCelular.getText()))) {
            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.campo_invalido), Toast.LENGTH_SHORT).show();
            etCelular.requestFocus();
            return false;
        }
        else if (isEmailInvalido(String.valueOf(etEmail.getText()))) {
            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.campo_invalido), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }
        else if (isCampoVazio(String.valueOf(etSenha.getText()))) {
            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etSenha.requestFocus();
            return false;
        }
        else if (isCampoVazio(String.valueOf(etRepetirSenha.getText()))) {
            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etRepetirSenha.requestFocus();
            return false;
        }
        else if (!isCamposIguais(String.valueOf(etSenha.getText()), String.valueOf(etRepetirSenha.getText()))) {
            Toast.makeText(CadastroUsuarioActivity.this, getString(R.string.senhas_diferentes), Toast.LENGTH_SHORT).show();
            etSenha.requestFocus();
            return false;
        }

        return true;
    }
}