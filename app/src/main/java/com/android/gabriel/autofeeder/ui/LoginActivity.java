package com.android.gabriel.autofeeder.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gabriel.autofeeder.R;
import com.android.gabriel.autofeeder.api.UsuarioApi;
import com.android.gabriel.autofeeder.model.Usuario;
import com.android.gabriel.autofeeder.model.UsuarioLogado;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.android.gabriel.autofeeder.utils.Util.*;

public class LoginActivity extends AppCompatActivity {

    private AutoCompleteTextView etEmail;
    private AutoCompleteTextView etSenha;
    private CheckBox cbLembrar;
    private Button btnLogar;
    private TextView tvRecuperarSenha;
    private Button btnCadastrar;

    private UsuarioLogado mUsuarioLogado;
    private UsuarioApi mUsuarioApi;
    private Retrofit mRetrofit;

    private static final String PREF_NAME = "login";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SENHA = "senha";
    private static final String KEY_LEMBRAR = "lembrar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        cbLembrar = findViewById(R.id.cbLembrar);
        btnLogar = findViewById(R.id.btnLogar);
        tvRecuperarSenha = findViewById(R.id.tvRecuperarSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(KEY_LEMBRAR, false)) {
            cbLembrar.setChecked(true);
        } else {
            cbLembrar.setChecked(false);
        }
        etEmail.setText(preferences.getString(KEY_EMAIL, ""));
        etSenha.setText(preferences.getString(KEY_SENHA, ""));

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mUsuarioApi = mRetrofit.create(UsuarioApi.class);

        btnLogar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validaCampos()) {
                    atualizarPreferences();

                    Call<Usuario> call = mUsuarioApi.login(
                            String.valueOf(etEmail.getText()),
                            String.valueOf(etSenha.getText()),
                            "password");

                    call.enqueue(new Callback<Usuario>() {
                        @Override
                        public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                            if (response.isSuccessful()) {
                                mUsuarioLogado = UsuarioLogado.getInstance();
                                mUsuarioLogado.setId(response.body().getId());
                                mUsuarioLogado.setFullName(response.body().getFullName());
                                mUsuarioLogado.setPhoneNumber(response.body().getPhoneNumber());
                                mUsuarioLogado.setEmail(response.body().getUserName());
                                mUsuarioLogado.setAccessToken(response.body().getAccessToken());

                                abrirTelaPrincipal();
                            }
                            else {
                                try {
                                    JSONObject jObjErro = new JSONObject(response.errorBody().string());
                                    Toast.makeText(LoginActivity.this, getString(R.string.erro) + " " + jObjErro.getString("error_description"), Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Toast.makeText(LoginActivity.this, getString(R.string.erro) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Usuario> call, Throwable t) {
                            Toast.makeText(LoginActivity.this, getString(R.string.sem_conexao_api), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        cbLembrar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                atualizarPreferences();
            }
        });

        tvRecuperarSenha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirTelaEsqueceuSenha();
            }
        });

        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirTelaCadastro();
            }
        });
    }

    public void abrirTelaCadastro() {
        Intent intent = new Intent(this, CadastroUsuarioActivity.class);
        startActivity(intent);
    }

    public void abrirTelaPrincipal() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void abrirTelaEsqueceuSenha() {
        Intent intent = new Intent(this, EsqueceuSenhaActivity.class);
        startActivity(intent);
    }

    private boolean validaCampos() {
        if (isEmailInvalido(String.valueOf(etEmail.getText()))) {
            Toast.makeText(LoginActivity.this, getString(R.string.campo_invalido), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        } else if (isCampoVazio(String.valueOf(etSenha.getText()))) {
            Toast.makeText(LoginActivity.this, getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etSenha.requestFocus();
            return false;
        }

        return true;
    }

    private void atualizarPreferences() {
        if (cbLembrar.isChecked()) {
            if (validaCampos()) {
                SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(KEY_EMAIL, etEmail.getText().toString().trim());
                editor.putString(KEY_SENHA, etSenha.getText().toString().trim());
                editor.putBoolean(KEY_LEMBRAR, true);
                editor.apply();
            } else {
                cbLembrar.setChecked(false);
            }
        } else {
            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_SENHA);
            editor.putBoolean(KEY_LEMBRAR, false);
            editor.apply();
        }
    }
}