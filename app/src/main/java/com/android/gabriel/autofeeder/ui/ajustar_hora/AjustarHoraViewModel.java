package com.android.gabriel.autofeeder.ui.ajustar_hora;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class AjustarHoraViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public AjustarHoraViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is tools fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}