package com.android.gabriel.autofeeder.ui.conectar;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class ConectarViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ConectarViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Dispositivos Pareados");
    }

    public LiveData<String> getText() {
        return mText;
    }
}