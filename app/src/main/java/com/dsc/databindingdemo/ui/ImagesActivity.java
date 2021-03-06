package com.dsc.databindingdemo.ui;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.dsc.databindingdemo.R;
import com.dsc.databindingdemo.adapter.SimpleSheetAdapter;
import com.dsc.databindingdemo.core.MyBaseActivity;
import com.dsc.databindingdemo.databinding.ActivityImagesBinding;
import com.dsc.databindingdemo.model.custom.ImgsInfo;
import com.dsc.databindingdemo.model.event.RvScrollEvent;
import com.dsc.databindingdemo.utils.DateTimeUtils;
import com.dsc.databindingdemo.utils.FileUtils;
import com.dsc.databindingdemo.utils.FrescoUtils;
import com.dsc.databindingdemo.utils.ToastUtil;
import com.dsc.databindingdemo.widget.DividerItemDecoration;
import com.dsc.databindingdemo.widget.ImageLoadingDrawable;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.image.ImageInfo;
import com.michaelflisar.rxbus2.RxBus;
import com.michaelflisar.rxbus2.RxBusBuilder;
import com.reny.mvpvmlib.BaseViewModel;
import com.reny.mvpvmlib.EmptyPresenter;
import com.reny.mvpvmlib.utils.SwipeBackUtils;

import java.util.ArrayList;
import java.util.List;

import cn.bingoogolapple.androidcommon.adapter.BGAOnRVItemClickListener;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import me.relex.photodraweeview.OnViewTapListener;
import me.relex.photodraweeview.PhotoDraweeView;

public class ImagesActivity extends MyBaseActivity<ActivityImagesBinding, BaseViewModel, EmptyPresenter> {

    private String TAG = getClass().getSimpleName();
    private DraweePagerAdapter draweePagerAdapter;
    private boolean isListeningChanged = false;//是否监听数据变化，默认不监听
    public List<String> imgsList;

    @Override
    protected void init(Bundle savedInstanceState) {
        SwipeBackUtils.DisableSwipeActivity(this);
        binding.flRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if(null != getIntent()){
            ImgsInfo imgsInfo = (ImgsInfo) getIntent().getSerializableExtra(ImgsInfo.KEY);
            if(null == imgsInfo)return;

            isListeningChanged = imgsInfo.isListeningChanged();
            binding.vpImg.setPageMargin((int) (getResources().getDisplayMetrics().density * 15));

            draweePagerAdapter = new DraweePagerAdapter();
            imgsList = imgsInfo.getImgsList();
            draweePagerAdapter.setImgsUrl(imgsList);

            binding.vpImg.setAdapter(draweePagerAdapter);
            binding.vpImg.setCurrentItem(imgsInfo.getCurPos());
            if(isListeningChanged) {//如果需要监听变化，就将浏览的位置传递出去,例如可以加载新图片数据并传递进来更新
                if(imgsInfo.getCurPos() > imgsInfo.getImgsList().size() - 4) {
                    RxBus.get().send(new RvScrollEvent(TAG, imgsInfo.getCurPos()));
                }
                binding.vpImg.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position) {
                        RxBus.get().send(new RvScrollEvent(TAG, position));
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                });
            }
        }

        if(isListeningChanged) {
            //监听数据是否更新
            presenter.addDisposable(RxBusBuilder.create(ImgsInfo.class)
                    .subscribe(new Consumer<ImgsInfo>() {
                        @Override
                        public void accept(ImgsInfo imgsInfo) {
                            if(null == draweePagerAdapter)return;
                            imgsList = imgsInfo.getImgsList();
                            draweePagerAdapter.setImgsUrl(imgsList);
                            draweePagerAdapter.notifyDataSetChanged();
                        }
                    }));
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_images;
    }

    @Override
    protected Class<EmptyPresenter> getPresenterClass() {
        return EmptyPresenter.class;
    }

    @Override
    protected Class<BaseViewModel> getViewModelClass() {
        return null;
    }

    private class DraweePagerAdapter extends PagerAdapter {

        private List<String> imgsUrl;

        private RecyclerView rv;
        private SimpleSheetAdapter adapter;
        private BottomSheetDialog sheetDialog;
        private List<String> sheetNames;

        private void setImgsUrl(List<String> imgsUrl){
            this.imgsUrl = imgsUrl;
        }

        @Override
        public int getCount() {
            return imgsUrl.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup viewGroup, final int position) {
            final PhotoDraweeView photoDraweeView = new PhotoDraweeView(viewGroup.getContext());
            photoDraweeView.getHierarchy().setProgressBarImage(new ImageLoadingDrawable());
            photoDraweeView.getHierarchy().setFailureImage(getResources().getDrawable(R.color.img_error_color), ScalingUtils.ScaleType.FIT_CENTER);
            PipelineDraweeControllerBuilder controller = Fresco.newDraweeControllerBuilder();
            controller.setAutoPlayAnimations(true);
            controller.setUri(Uri.parse(imgsUrl.get(position)));
            controller.setOldController(photoDraweeView.getController());
            controller.setControllerListener(new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    super.onFinalImageSet(id, imageInfo, animatable);
                    if (imageInfo == null) {
                        return;
                    }
                    photoDraweeView.update(imageInfo.getWidth(), imageInfo.getHeight());
                }
            });
            photoDraweeView.setController(controller.build());
            photoDraweeView.setOnViewTapListener(new OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    finish();
                }
            });
            photoDraweeView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longPressImg(v, position);
                    return false;
                }
            });

            try {
                viewGroup.addView(photoDraweeView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            } catch (Exception e) {e.printStackTrace();}

            return photoDraweeView;
        }

        private void longPressImg(View v, final int imgPos){
            Context context = v.getContext();
            if(null == rv){
                rv = new RecyclerView(context);
                rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                rv.setLayoutManager(new MyLinearLayoutManager(context));
                rv.addItemDecoration(DividerItemDecoration.get1pxDividerV(context));
            }
            if(null == sheetNames) sheetNames = new ArrayList<>();
            else sheetNames.clear();

            sheetNames.add("保存图片");
            sheetNames.add("取消");

            if (null == adapter) {
                adapter = new SimpleSheetAdapter(rv);
                rv.setAdapter(adapter);
            }
            else adapter.notifyDataSetChanged();
            adapter.setData(sheetNames);
            if (null == sheetDialog) {
                sheetDialog = new BottomSheetDialog(context);
                sheetDialog.setContentView(rv);
            }
            sheetDialog.show();

            adapter.setOnRVItemClickListener(new BGAOnRVItemClickListener() {
                @Override
                public void onRVItemClick(ViewGroup parent, View itemView, int position) {
                    if (sheetDialog.isShowing()) sheetDialog.dismiss();
                    if (position == 0) {
                        //简单处理权限问题
                        presenter.addDisposable(
                            presenter.getRxPermissions()
                                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .subscribeWith(new DisposableObserver<Boolean>() {
                                        @Override
                                        public void onNext(Boolean granted) {
                                            if(granted){
                                                String dir = FileUtils.getDownLoadImgPath();
                                                if (dir != null) {
                                                    String imgName = "img_" + DateTimeUtils.getCurDateStr(DateTimeUtils.getFormatYMDHMStimeStamp());
                                                    FrescoUtils.savePicture(imgsUrl.get(imgPos), dir, imgName);
                                                } else {
                                                    ToastUtil.showShort(getResources().getString(R.string.have_no_storage));
                                                }
                                            }else {
                                                ToastUtil.showLong(getResources().getString(R.string.no_permission_WRITE_EXTERNAL));
                                            }
                                        }
                                        @Override
                                        public void onError(Throwable e) {
                                        }
                                        @Override
                                        public void onComplete() {}
                                    })
                        );
                    }
                }
            });
        }
    }

    private class MyLinearLayoutManager extends LinearLayoutManager {
        private MyLinearLayoutManager(Context context) {
            super(context);
        }
        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }
}