package com.dsc.databindingdemo.ui;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.dsc.databindingdemo.R;
import com.dsc.databindingdemo.core.BaseFragment;
import com.dsc.databindingdemo.databinding.FragmentABinding;
import com.dsc.databindingdemo.presenter.HomePresenter;
import com.dsc.databindingdemo.presenter.vm.HomeViewModel;

/**
 * Created by reny on 2017/1/9.
 */

public class FragmentA extends BaseFragment<FragmentABinding, HomeViewModel, HomePresenter> {

    @Override
    protected void onCreateViewLazy(Bundle savedInstanceState) {
        binding.setPresenter(presenter);
        binding.setViewModel(viewModel);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_a;
    }

    @Override
    protected Class<HomeViewModel> getViewModelClass() {
        return HomeViewModel.class;
    }

    @Override
    protected Class<HomePresenter> getPresenterClass() {
        return HomePresenter.class;
    }

}