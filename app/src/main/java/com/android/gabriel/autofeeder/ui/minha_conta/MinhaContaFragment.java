package com.android.gabriel.autofeeder.ui.minha_conta;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

public class MinhaContaFragment extends Fragment {

    private MinhaContaViewModel minhaContaViewModel;

    private EditText etNome;
    private EditText etCelular;
    private EditText etEmail;
    private EditText etSenhaAtual;
    private EditText etNovaSenha;
    private EditText etRepetirNovaSenha;
    private Button btnConfirmar;

    private UsuarioApi mUsuarioApi;
    private Retrofit mRetrofit;
    private UsuarioLogado mUsuarioLogado;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        minhaContaViewModel =
                ViewModelProviders.of(this).get(MinhaContaViewModel.class);
        View root = inflater.inflate(R.layout.fragment_minha_conta, container, false);

        etNome = root.findViewById(R.id.etNome);
        etCelular = root.findViewById(R.id.etCelular);
        etEmail = root.findViewById(R.id.etEmail);
        etSenhaAtual = root.findViewById(R.id.etSenhaAtual);
        etNovaSenha = root.findViewById(R.id.etNovaSenha);
        etRepetirNovaSenha = root.findViewById(R.id.etRepetirNovaSenha);
        btnConfirmar = root.findViewById(R.id.btnConfirmar);

        mRetrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mUsuarioApi = mRetrofit.create(UsuarioApi.class);

        mUsuarioLogado = mUsuarioLogado.getInstance();

        preencherCampos();

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validaCampos()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                    Usuario usuario = new Usuario();
                    usuario.setOldPassword(String.valueOf(etSenhaAtual.getText()));
                    usuario.setNewPassword(String.valueOf(etNovaSenha.getText()));
                    usuario.setConfirmPassword(String.valueOf(etRepetirNovaSenha.getText()));

                    Call<Void> call = mUsuarioApi.changePassword("Bearer " + mUsuarioLogado.getAccessToken(), usuario);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            builder.setTitle(R.string.titulo_mensagem);
                            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });

                            if (response.isSuccessful()) {
                                builder.setMessage(getString(R.string.salvo));
                                etSenhaAtual.setText("");
                                etNovaSenha.setText("");
                                etRepetirNovaSenha.setText("");
                            } else {
                                try {
                                    JSONObject jObjErro = new JSONObject(response.errorBody().string());
                                    builder.setMessage(jObjErro.getString("ModelState"));
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), getString(R.string.erro) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

                            builder.create();
                            builder.show();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getContext(), getString(R.string.sem_conexao_api), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        return root;
    }

    private void preencherCampos() {
        etNome.setText(mUsuarioLogado.getFullName());
        etCelular.setText(mUsuarioLogado.getPhoneNumber());
        etEmail.setText(mUsuarioLogado.getEmail());
    }

    private boolean validaCampos() {
        if (isCampoVazio(String.valueOf(etNome.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etNome.requestFocus();
            return false;
        }
        else if (isCelularInvalido(String.valueOf(etCelular.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_invalido), Toast.LENGTH_SHORT).show();
            etCelular.requestFocus();
            return false;
        }
        else if (isEmailInvalido(String.valueOf(etEmail.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_invalido), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }
        else if (isCampoVazio(String.valueOf(etSenhaAtual.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etSenhaAtual.requestFocus();
            return false;
        }
        else if (isCampoVazio(String.valueOf(etNovaSenha.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etNovaSenha.requestFocus();
            return false;
        }
        else if (isCampoVazio(String.valueOf(etRepetirNovaSenha.getText()))) {
            Toast.makeText(getContext(), getString(R.string.campo_vazio), Toast.LENGTH_SHORT).show();
            etRepetirNovaSenha.requestFocus();
            return false;
        }
        else if (!isCamposIguais(String.valueOf(etNovaSenha.getText()), String.valueOf(etRepetirNovaSenha.getText()))) {
            Toast.makeText(getContext(), getString(R.string.senhas_diferentes), Toast.LENGTH_SHORT).show();
            etNovaSenha.requestFocus();
            return false;
        }

        return true;
    }
}
