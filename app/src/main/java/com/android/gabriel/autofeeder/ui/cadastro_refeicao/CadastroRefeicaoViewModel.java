package com.android.gabriel.autofeeder.ui.cadastro_refeicao;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class CadastroRefeicaoViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public CadastroRefeicaoViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is send fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}