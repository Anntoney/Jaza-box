package com.tonney.shop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
//import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener;
import com.tonney.shop.adapter.CategoryProductAdapter;
import com.tonney.shop.entity.ProductObject;
import com.tonney.shop.network.GsonRequest;
import com.tonney.shop.network.VolleySingleton;
import com.tonney.shop.utils.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryProductActivity extends AppCompatActivity {


    private static final String TAG = CategoryProductActivity.class.getSimpleName();
    private ProgressBar progressBar;
    private boolean isLoading = true;
    private int pageNumer = 1;
    private   int pastVisibleItems,visibleItemCount,totalItemCount,previous_total = 0;
    private int view_threshold = 10;


    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_product);


        progressBar = (ProgressBar) findViewById(R.id.pbar);

        int categoryId = getIntent().getExtras().getInt("CATEGORY_ID");
        String categoryName = getIntent().getExtras().getString("CATEGORY_NAME");

        setTitle(categoryName + " Category");

        recyclerView = (RecyclerView)findViewById(R.id.category_in_product);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        if(!Helper.isNetworkAvailable(this)){
            Helper.displayErrorMessage(this, getString(R.string.no_internet));
        }else{
            categoryProductList(String.valueOf(categoryId));
        }

    }



    private void categoryProductList(String id){
        Map<String, String> params = new HashMap<String,String>();
        params.put(Helper.ID, id);

        GsonRequest<ProductObject[]> serverRequest = new GsonRequest<ProductObject[]>(
                Request.Method.POST,
                Helper.PATH_TO_PRODUCT_IN_CATEGORY,
                ProductObject[].class,
                params,
                createRequestSuccessListener(),
                createRequestErrorListener());

        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                Helper.MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(this).addToRequestQueue(serverRequest);
    }






    private Response.Listener<ProductObject[]> createRequestSuccessListener() {
        return new Response.Listener<ProductObject[]>() {
            @Override
            public void onResponse(ProductObject[] response) {
                try {
                    if(response.length > 0){
                        //display restaurant menu information
                        List<ProductObject> productList = new ArrayList<>();
                        for(int i = 0; i < response.length; i++){
                            productList.add(response[i]);
                        }
                        CategoryProductAdapter mAdapter = new CategoryProductAdapter(CategoryProductActivity.this, productList);
                        recyclerView.setAdapter(mAdapter);
                    }else{
                        Helper.displayErrorMessage(CategoryProductActivity.this, "No product in this category yet ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener createRequestErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        };
    }




}
