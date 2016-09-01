package cn.jianke.jkstepsensor.common.data;

import android.content.Context;
import java.util.ArrayList;
import java.util.Date;
import cn.jianke.customcache.data.ListCache;
import cn.jianke.customcache.data.SpLocalCache;
import cn.jianke.jkstepsensor.common.data.bean.StepModel;
import cn.jianke.jkstepsensor.common.utils.DateUtils;
public class DataCache {
    private static DataCache instance;
    private SpLocalCache<ListCache> mSpLocalCache;
    private ArrayList<StepModel> mCacheList;
    private ListCache<StepModel> mListCache;

    private DataCache(){
        mListCache = new ListCache<>();
        mCacheList = new ArrayList<>();
        mSpLocalCache = new SpLocalCache<>(ListCache.class, StepModel.class);
    }

    public synchronized static DataCache getInstance(){
        if (instance == null)
            instance = new DataCache();

        return instance;
    }

    public void addStepCache(Context context, final StepModel mStepModel){
        if (mSpLocalCache != null){
            mSpLocalCache.read(context, new SpLocalCache.LocalCacheCallBack() {
                @Override
                public void readCacheComplete(Object obj) {
                    if (obj != null){
                        mListCache = (ListCache<StepModel>) obj;
                        if (mListCache != null) {
                            mCacheList = mListCache.getObjList();
                            if (mCacheList == null || mCacheList.size() == 0){
                                mCacheList.add(mStepModel);
                            }
                            for (StepModel stepModel : mCacheList) {
                                if (mStepModel.getDate().equals(stepModel.getDate()) ) {
                                    int cha = Integer.parseInt(mStepModel.getStep())
                                            - Integer.parseInt(stepModel.getStep());
                                    if (cha >= 0) {
                                        mCacheList.remove(stepModel);
                                        mCacheList.add(mStepModel);
                                    }
                                    break;
                                }
                            }
                        }
                    }else {
                        mCacheList.add(mStepModel);
                    }
                }
            });
            mListCache.setObjList(mCacheList);
            mSpLocalCache.save(context, mListCache);
        }
    }

    public void getTodayCache(Context context,DataCacheListener mDataCacheListener){
        getCacheByDate(context, new Date(), mDataCacheListener);
    }

    public void getCacheByDate(Context context, Date date, final DataCacheListener mDataCacheListener){
        final String dateStr = DateUtils.simpleDateFormat(date);
        if (mSpLocalCache != null){
            mSpLocalCache.read(context, new SpLocalCache.LocalCacheCallBack() {
                @Override
                public void readCacheComplete(Object obj) {
                    if (obj != null){
                        mListCache = (ListCache<StepModel>) obj;
                        if (mListCache != null && mDataCacheListener != null){
                            mCacheList = mListCache.getObjList();
                            for (StepModel stepModel : mCacheList) {
                                if (dateStr.equals(stepModel.getDate())) {
                                    mDataCacheListener.readListCache(stepModel);
                                    return;
                                }
                            }
                        }
                    }
                    StepModel model = new StepModel();
                    model.setDate(dateStr);
                    model.setStep(0 + "");
                    mDataCacheListener.readListCache(model);
                }
            });
        }
    }

    public void clearAllCache(Context context){
        if (mSpLocalCache != null){
            mSpLocalCache.clear(context);
        }
    }

    public void clearTodayData(Context context){
        clearCacheByDate(context, new Date());
    }

    public void clearCacheByDate(Context context, Date date){
        final String dateStr = DateUtils.simpleDateFormat(date);
        if (mSpLocalCache != null){
            mSpLocalCache.read(context, new SpLocalCache.LocalCacheCallBack() {
                @Override
                public void readCacheComplete(Object obj) {
                    if (obj != null){
                        mListCache = (ListCache<StepModel>) obj;
                        if (mListCache != null) {
                            mCacheList = mListCache.getObjList();
                            for (StepModel stepModel : mCacheList) {
                                if (dateStr.equals(stepModel.getDate())) {
                                    mCacheList.remove(stepModel);
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    public interface DataCacheListener{
        void readListCache(StepModel stepModel);
    }
}
