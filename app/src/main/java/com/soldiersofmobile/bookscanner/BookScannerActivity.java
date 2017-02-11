package com.soldiersofmobile.bookscanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.soldiersofmobile.bookscanner.api.BookApi;
import com.soldiersofmobile.bookscanner.api.model.BooksResponse;
import com.soldiersofmobile.bookscanner.api.model.VolumeInfo;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BookScannerActivity extends AppCompatActivity {

    public static final String VOLUME_INFO_EXTRA = "volume_info";
    private static final String TAG = BookScannerActivity.class.getSimpleName();

    @BindView(R.id.isbn_edit_text)
    EditText isbnEditText;
    @BindView(R.id.scan_button)
    Button scanButton;
    @BindView(R.id.get_book_details_button)
    Button getBookDetailsButton;
    @BindView(R.id.recent_books_list_view)
    ListView recentBooksListView;
    @BindView(R.id.activity_main)
    RelativeLayout activityMain;
    private ArrayAdapter<VolumeInfo> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_scanner);
        ButterKnife.bind(this);
        isbnEditText.setText("9780132350884"); //TODO remove me, only for tests
        adapter = new ArrayAdapter<VolumeInfo>(this, android.R.layout.simple_list_item_1);
        recentBooksListView.setAdapter(adapter);
    }

    @OnClick(R.id.get_book_details_button)
    public void onClick() {

        getDetails();
    }

    public void getDetails() {

        String query = "isbn:" + isbnEditText.getText().toString();
        BookApi bookApi = getBookApi();
        final Call<BooksResponse> book = bookApi.getBook(query);

        book.enqueue(new Callback<BooksResponse>() {
            @Override
            public void onResponse(Call<BooksResponse> call, Response<BooksResponse> response) {
                if (response.isSuccessful()) {
                    BooksResponse booksResponse = response.body();
                    if (booksResponse.getTotalItems() > 0) {
                        VolumeInfo volumeInfo = booksResponse.getItems().get(0).getVolumeInfo();
                        //adapter.add(volumeInfo); //TODO prepare Adapter and list to display data

                        //send VolumeInfo to the BookDetailsActivity
                        //TODO create activity and uncomment this code
                        Intent intent = new Intent(BookScannerActivity.this, BookDetailsActivity.class);
                        intent.putExtra(VOLUME_INFO_EXTRA, volumeInfo);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onFailure(Call<BooksResponse> call, Throwable t) {
                Log.e(TAG, "Can't get book data", t);
            }
        });
    }

    private BookApi getBookApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(BookApi.class);
    }

    //start barcode scanner activity
    public void scan() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.initiateScan();
    }

    //return from scanner, fill the field with the isbn
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            isbnEditText.setText(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick({R.id.scan_button, R.id.get_book_details_button})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan_button:
                scan();
                break;
            case R.id.get_book_details_button:
                getDetails();
                break;
        }
    }
}
