package com.cmpe277.onlinemilktea.Interface;

import com.cmpe277.onlinemilktea.Model.PopularCategoryModel;

import java.util.List;

public interface IPopularCallbackListener {
    void onPopularLoadSuccess(List<PopularCategoryModel> popularCategoryModels);
    void onPopularLoadFailed(String message);
}
