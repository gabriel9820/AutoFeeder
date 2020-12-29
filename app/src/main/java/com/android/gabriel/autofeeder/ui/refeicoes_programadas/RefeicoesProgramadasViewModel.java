package com.android.gabriel.autofeeder.ui.refeicoes_programadas;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class RefeicoesProgramadasViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public RefeicoesProgramadasViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}