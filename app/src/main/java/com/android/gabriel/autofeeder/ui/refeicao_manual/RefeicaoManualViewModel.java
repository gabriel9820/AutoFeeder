package com.android.gabriel.autofeeder.ui.refeicao_manual;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class RefeicaoManualViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public RefeicaoManualViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}