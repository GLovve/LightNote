package com.simple.lightnote.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.simple.lightnote.LightNoteApplication;
import com.simple.lightnote.R;
import com.simple.lightnote.activities.base.BaseSwipeActivity;
import com.simple.lightnote.constant.SPConstans;
import com.simple.lightnote.db.DaoSession;
import com.simple.lightnote.db.NoteDao;
import com.simple.lightnote.model.SimpleNote;
import com.simple.lightnote.util.SPUtil;
import com.simple.lightnote.utils.ListUtils;
import com.simple.lightnote.utils.LogUtils;
import com.simple.lightnote.utils.MD5Utils;
import com.simple.lightnote.utils.ToastUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 编辑页面
 *
 * @author homelink
 */
public class SimpleNoteEditActivity extends BaseSwipeActivity {
    private static final String TAG = "SimpleNoteEditActivity";

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.simpleNote_edt_noteContent)
    EditText edt_noteContent;
    @Bind(R.id.simpleNote_ll_actionbar)
    LinearLayout ll_acitonBar;


    private NoteDao noteDao;
    private String s_noteContent;
    private String md5;
    private SimpleNote note;
    /**
     * 笔记是否改变
     */
    private boolean textChanged;
    private String noteId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_simplenoteedit);
        ButterKnife.bind(this);
        initDB();
        initView();
        initListener();
        initData();
//        loadData();

    }

    /***
     * 从数据库与网络加载数据
     */
    private void loadData() {
        // TODO: 2016/8/18 从多个数据源获取数据
        Observable<SimpleNote> databases = Observable.empty();
        Observable<SimpleNote> network = Observable.empty();
        Observable<SimpleNote> source = Observable
                .concat(databases, network)
                .first();
        Subscription subscribe = source.subscribe(new Action1<SimpleNote>() {
            @Override
            public void call(SimpleNote note) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO 保存文件
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                saveToDB();
            }
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(new Subscriber<Object>() {
            @Override
            public void onCompleted() {
                textChanged = false;
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Object o) {

            }
        });
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                com.evernote.edam.type.Note note = new com.evernote.edam.type.Note();
                note.setContent(edt_noteContent.getText().toString().trim());
                EvernoteSession.getInstance().getEvernoteClientFactory().getNoteStoreClient().createNoteAsync(note, new EvernoteCallback<com.evernote.edam.type.Note>() {
                    @Override
                    public void onSuccess(com.evernote.edam.type.Note result) {

                    }

                    @Override
                    public void onException(Exception exception) {

                    }
                });
            }
        });
    }

    private void initListener() {
        edt_noteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initDB() {
        LightNoteApplication application = (LightNoteApplication) getApplication();
        DaoSession daoSession = application.getDaoSession();
        noteDao = daoSession.getNoteDao();

    }

    private void initData() {
        String guid = getIntent().getStringExtra("noteId");
        Log.e(TAG, "initData: " + this.noteId);
        if (!TextUtils.isEmpty(guid)) {

            Observable.create(new Observable.OnSubscribe<SimpleNote>() {
                @Override
                public void call(Subscriber<? super SimpleNote> subscriber) {
                    List<SimpleNote> list = noteDao.queryBuilder().where(NoteDao.Properties.guid.eq(guid)).build().list();
                    LogUtils.e(TAG, "call:  " +"从数据库取的list size: "+list.size()+"list content :"+list);
                    if (!ListUtils.isEmpty(list)) {
                        SimpleNote note = list.get(0);
                        subscriber.onNext(note);
                    }
                }
            }).observeOn(Schedulers.io()).subscribeOn(AndroidSchedulers.mainThread()).subscribe(note1 -> {
                String noteContent = note.getContent();
                if (!TextUtils.isEmpty(noteContent)) {
                    edt_noteContent.setText(noteContent);
                    edt_noteContent.setSelection(noteContent.length());
                }
            });


        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        int showToolBar = SPUtil.getInstance(this).getInt(SPConstans.EDIT_TOOL_BAR, -1);
        setToolBarVisible(showToolBar);
    }

    private void initView() {
        mToolbar.setTitle("编辑");
        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_simplenoteedit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void saveToDB() {
        s_noteContent = edt_noteContent.getText().toString().trim();
        md5 = MD5Utils.MD5Encode(s_noteContent);
        if (note != null) {
            //更新
            if (!note.getContent().trim().equals(s_noteContent)) {
                note.setTitle(note.getTitle());
                note.setCreated(note.getCreated());
                note.setUpdated(System.currentTimeMillis());
                note.setContent(s_noteContent);
                LogUtils.e(TAG, "更新");
                LogUtils.e(TAG, note);
                insertAndUpdate();
            }
        } else {
            //新建
            if (!TextUtils.isEmpty(s_noteContent)) {
                note = new SimpleNote();
                note.setCreated(System.currentTimeMillis());
                note.setUpdated(System.currentTimeMillis());
                note.setTitle(note.getTitle());
                note.setContent(s_noteContent);
                LogUtils.e(TAG, "新建");
                LogUtils.e(TAG, note);
                insertAndUpdate();
            }

        }
    }

    private void insertAndUpdate() {
        ToastUtils.showToast(SimpleNoteEditActivity.this, "正在保存");
        Observable
                .just(note)
                .filter(new Func1<SimpleNote, Boolean>() {
                    @Override
                    public Boolean call(SimpleNote note) {
                        return note != null;
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        noteId = note.getGuid();
                        if (noteId == null) {
                            long insert = noteDao.insert(note);
                        } else {
                            noteDao.update(note);
                        }


                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SimpleNote>() {
                    @Override
                    public void onCompleted() {
                        LogUtils.d(TAG, "completed");
                        textChanged = false;
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(SimpleNote note) {
                        onCompleted();
                    }
                });


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_simplenoteedit_actionBar:

                int flag_actionBar = -1;
                if (ll_acitonBar.getVisibility() == View.VISIBLE) {
                    setToolBarVisible(1);
                    flag_actionBar = 1;
                } else {
                    setToolBarVisible(0);
                    flag_actionBar = 0;
                }
                SPUtil.getEditor(SimpleNoteEditActivity.this).putInt(SPConstans.EDIT_TOOL_BAR, flag_actionBar).apply();
                return true;

            case R.id.menu_simplenoteedit_caption:
                ToastUtils.showToast(SimpleNoteEditActivity.this, "caption");
                return true;
            case R.id.menu_simplenoteedit_orderlist:
                ToastUtils.showToast(SimpleNoteEditActivity.this, "有序");
                return true;
            case R.id.menu_simplenoteedit_list:
                ToastUtils.showToast(SimpleNoteEditActivity.this, "清单");
                return true;
            case R.id.menu_simplenoteedit_info:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void setToolBarVisible(int i) {
        if (i == 0) {
            ll_acitonBar.setVisibility(View.VISIBLE);
            AlphaAnimation alpha = new AlphaAnimation(0, 1);
            alpha.setDuration(300);
            alpha.setFillAfter(true);
            ll_acitonBar.startAnimation(alpha);
        } else {
            ll_acitonBar.setVisibility(View.GONE);
            AlphaAnimation alpha = new AlphaAnimation(1, 0);
            alpha.setDuration(300);
            alpha.setFillAfter(true);
            ll_acitonBar.startAnimation(alpha);
        }
    }

    public void onToolBarClick(View v) {
        String trim = edt_noteContent.getText().toString().trim();

        switch (v.getId()) {
            case R.id.edit_toolbar_item_1:
                if (trim.length() > 0) {
//                    int selectionStart = edt_noteContent.getSelectionStart();
                    int selectionEnd = edt_noteContent.getSelectionEnd();
                    if (trim.trim().lastIndexOf('#') == trim.length() - 2) {
                        edt_noteContent.append("#");
                    } else {
                        edt_noteContent.append("# ");
                    }
                } else {
                    edt_noteContent.append("# ");
                }

                break;
            case R.id.edit_toolbar_item_2:
                break;
            case R.id.edit_toolbar_item_3:
                break;
            case R.id.edit_toolbar_item_4:
                break;
            case R.id.edit_toolbar_item_5:
                int selectionEnd = edt_noteContent.getSelectionEnd();
                if (selectionEnd > 1) {
                    edt_noteContent.append("\n---\n", selectionEnd - 1, selectionEnd);
                } else {
                    edt_noteContent.append("---\n");
                }

                break;
            case R.id.edit_toolbar_item_save:
                if (textChanged) {
                    saveToDB();
                }
                if (!TextUtils.isEmpty(trim)) {
                    Intent intent = new Intent(this, NotePreViewActivity.class);
                    intent.putExtra("sourceType", NotePreViewActivity.Source_id);
                    intent.putExtra("noteId", noteId);
                    startActivity(intent);
                } else {

                    finish();
                }
                break;
        }
    }

}
