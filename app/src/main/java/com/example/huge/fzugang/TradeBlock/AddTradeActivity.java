package com.example.huge.fzugang.TradeBlock;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.example.huge.fzugang.R;
import com.example.huge.fzugang.RetrofitStuff.RetrofitUtil;
import com.example.huge.fzugang.Utils.FzuGangEditTextWatcher;
import com.example.huge.fzugang.Utils.FzuGangTextInputWatcher;
import com.example.huge.fzugang.Utils.LoadingdialogUtil;
import com.example.huge.fzugang.Utils.SharedPreferencesUtil;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import com.zyao89.view.zloading.ZLoadingDialog;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTradeActivity extends AppCompatActivity{
    @BindView(R.id.add_trade_post_button)
    Button addButton;
    @BindView(R.id.add_trade_title)
    TextInputLayout title;
    @BindView(R.id.add_trade_content)
    TextInputLayout content;
    @BindView(R.id.add_trade_price)
    TextInputLayout price;
    @BindView(R.id.add_trade_fineness)
    TextInputLayout fineness;
    @BindView(R.id.add_trade_contact)
    TextInputLayout contact;
    @BindView(R.id.add_trade_pick_picture)
    GridView addPictureGrid;
    AddPictureGridViewAdapter addPictureAdapter;

    List<Uri> pictures=new ArrayList<>();//图片路径列表

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trade);

        ButterKnife.bind(this);
        init();
    }

    private void init(){
        //设置图片添加adapter
        addPictureAdapter=new AddPictureGridViewAdapter(pictures,this);
        addPictureGrid.setAdapter(addPictureAdapter);

        //设置Gridview点击事件
        addPictureGrid.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView,View view,int position,long id){
                Matisse.from(AddTradeActivity.this)
                        .choose(MimeType.allOf())
                        .countable(true)//true:选中后显示数字;false:选中后显示对号
                        .maxSelectable(9)//可选的最大数
                        .capture(false)//选择照片时，是否显示拍照
                        .captureStrategy(new CaptureStrategy(true,"com.example.robin.papers"))//参数1 true表示拍照存储在共有目录，false表示存储在私有目录；参数2与 AndroidManifest中authorities值相同，用于适配7.0系统 必须设置
                        .imageEngine(new GlideEngine())//图片加载引擎
                        .forResult(1);
            }
        });

        //先让按钮失效
        addButton.setEnabled(false);
        //设置文本框改变监听
        title.getEditText().addTextChangedListener(new FzuGangTextInputWatcher(this,addButton,title,content,price,fineness,contact));
        content.getEditText().addTextChangedListener(new FzuGangTextInputWatcher(this,addButton,title,content,price,fineness,contact));
        price.getEditText().addTextChangedListener(new FzuGangTextInputWatcher(this,addButton,title,content,price,fineness,contact));
        fineness.getEditText().addTextChangedListener(new FzuGangTextInputWatcher(this,addButton,title,content,price,fineness,contact));
        contact.getEditText().addTextChangedListener(new FzuGangTextInputWatcher(this,addButton,title,content,price,fineness,contact));

        //发布按钮监听
        addButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String titleTrans="";
                String contentTrans="";
                String finenessTrans="";
                String priceTrans="";
                String contactTrans="";
                String token=SharedPreferencesUtil.getStoredMessage(AddTradeActivity.this,"token");
                Map<String,RequestBody> tradeText=new HashMap<>();
                RequestBody tokenRequest=RequestBody.create(MediaType.parse("multipart/form-data"),token);
                RequestBody titleRequest=RequestBody.create(MediaType.parse("multipart/form-data"),title.getEditText().getText().toString());
                RequestBody contentRequest=RequestBody.create(MediaType.parse("multipart/form-data"),content.getEditText().getText().toString());
                RequestBody finenessRequest=RequestBody.create(MediaType.parse("multipart/form-data"),fineness.getEditText().getText().toString());
                RequestBody priceRequest=RequestBody.create(MediaType.parse("multipart/form-data"),price.getEditText().getText().toString());
                RequestBody contactRequest=RequestBody.create(MediaType.parse("multipart/form-data"),contact.getEditText().getText().toString());
                tradeText.put("token",tokenRequest);
                tradeText.put("title",titleRequest);
                tradeText.put("content",contentRequest);
                tradeText.put("fineness",finenessRequest);
                tradeText.put("price",priceRequest);
                tradeText.put("contact",contactRequest);
                tradeText.put("place",null);
                tradeText.put("classify",null);
                ZLoadingDialog zLoadingDialog=LoadingdialogUtil.getZLoadingDialog(AddTradeActivity.this);
                RetrofitUtil.postAddTrade(tradeText,getImgList(pictures),zLoadingDialog);
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        if(requestCode==1){
            if(resultCode==RESULT_OK){
                pictures=Matisse.obtainResult(data);
                addPictureAdapter.notifyDataSetChanged(pictures);
            }
        }
    }

    //将contentUri转化为发送请求时的数据结构(发送图片)
    private List<MultipartBody.Part> getImgList(List<Uri> origin){
        List<MultipartBody.Part> result=new ArrayList<>();
        for(Uri i: origin){
            //将contentUri转化为真实路径Uri
            String res=null;
            String[] proj={MediaStore.Images.Media.DATA};
            Cursor cursor=getContentResolver().query(i,proj,null,null,null);
            if(cursor.moveToFirst()){
                int column_index=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                res=cursor.getString(column_index);
            }
            cursor.close();
            //将路径uri转化为file
            File file=new File(res);
            //将路径file转化为RequestBody
            RequestBody requestBody=RequestBody.create(MediaType.parse("multipart/form-data"),file);
            //将RequestBody转化为MultipartBody.Part
            MultipartBody.Part finalRequest=MultipartBody.Part.createFormData("pics[]",file.getName(),requestBody);//pics[]为后端的key
            result.add(finalRequest);
        }
        return result;
    }

}