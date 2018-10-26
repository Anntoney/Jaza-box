package com.tonney.shop;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ServiceWorkerWebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;
import com.tonney.shop.entity.SuccessObject;
import com.tonney.shop.mpesa.Request;
import com.tonney.shop.network.GsonRequest;
import com.tonney.shop.network.VolleySingleton;
import com.tonney.shop.utils.CustomApplication;
import com.tonney.shop.utils.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.tonney.shop.CheckoutActivity.subTotal;
import static com.tonney.shop.utils.Helper.MY_SOCKET_TIMEOUT_MS;

public class MpesaConfrimation extends AppCompatActivity {


    EditText etPhoneNumber, etMpesaCode;
    Button btnComplete;

    ArrayList<String> arrMpesaCode;
    ArrayList<String> arrPhoneNumber;

    String url = "http://patchcreate.com/fetch/fetch.php";

    private String postContent;


    String strPhoneNumber, strMpesaCode;
    SweetAlertDialog sweetAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpesa_confrimation);


        etPhoneNumber = (EditText) findViewById(R.id.etMpesaNumberr);
        etMpesaCode = (EditText) findViewById(R.id.etMpesaCode);
        btnComplete = (Button) findViewById(R.id.btnComplete);
        sweetAlertDialog = new SweetAlertDialog(MpesaConfrimation.this, SweetAlertDialog.PROGRESS_TYPE);

        postContent = getIntent().getExtras().getString("MPESA_PAYMENT");


        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                arrMpesaCode = new ArrayList<>();
                arrPhoneNumber = new ArrayList<>();
                strPhoneNumber = etPhoneNumber.getText().toString().trim();
                strMpesaCode = etMpesaCode.getText().toString().trim();

                if (strMpesaCode.length() == 0) {

                    Toast.makeText(MpesaConfrimation.this, "Enter Mpesa Code", Toast.LENGTH_SHORT).show();
                } else if (strPhoneNumber.length() == 0) {

                    Toast.makeText(MpesaConfrimation.this, "Enter Phone NUmber", Toast.LENGTH_SHORT).show();
                } else {

                    makePayement(strPhoneNumber, strMpesaCode);
                }


            }
        });


    }

    private void makePayement(final String strPhoneNumber, final String strMpesaCode) {

        sweetAlertDialog.setTitleText("Processing payment...");
        sweetAlertDialog.setContentText("Confirming payment..");
        sweetAlertDialog.setCancelable(false);
        sweetAlertDialog.show();


        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {

                        //Creating a json object of the current index
                        JSONObject jsonObject;

                        //Getting Image Url and title json object
                        jsonObject = jsonArray.getJSONObject(i);

                        //Getting Image and title from json object
                        arrMpesaCode.add(jsonObject.getString("mpesa_receipt"));
                        arrPhoneNumber.add(jsonObject.getString("mpesa_phone"));


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (arrPhoneNumber.contains(strPhoneNumber) && (arrMpesaCode.contains(strMpesaCode))) {
                    final String tokenString = "token";




                    sweetAlertDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE);

                    //                    SweetAlertDialog sweet = new SweetAlertDialog(MpesaConfrimation.this, SweetAlertDialog.SUCCESS_TYPE);
//
                    sweetAlertDialog.setTitleText("Order Successfull!");
                    sweetAlertDialog.setContentText("");
                    sweetAlertDialog.setConfirmText("OK");
                    sweetAlertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {


                            String[] serverValues = postContent.split(";");
                            postCheckoutOrderToRemoteServer(tokenString, serverValues[0], serverValues[1], serverValues[2], serverValues[3], serverValues[4],
                                    serverValues[5], serverValues[6], serverValues[7], serverValues[8]);


                        }
                    });


                } else {


                    sweetAlertDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE);
                        sweetAlertDialog.setTitleText("Oops...");
                        sweetAlertDialog.setConfirmText("OK");
                            sweetAlertDialog.setContentText("Something went wrong! Try again");
                            sweetAlertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismiss();
                                }
                            })      ;

                    Toast.makeText(MpesaConfrimation.this, "UUUPS", Toast.LENGTH_SHORT).show();


                }


            }


        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                sweetAlertDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setConfirmText("OK");
                sweetAlertDialog.setContentText("Slow Internet Connection...");
                sweetAlertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                })      ;

            }
        });


        RequestQueue requestQueue = Volley.newRequestQueue(MpesaConfrimation.this);
        requestQueue.add(stringRequest);

    }




    private void postCheckoutOrderToRemoteServer(String token, String userId, String quantity, String discount, String tax, String price,
                                                 String address, String shipping, String payment_method, String order_list){
        Map<String, String> params = new HashMap<String,String>();
        params.put("TOKEN", token);
        params.put("USER_ID", userId);
        params.put("QUANTITY", quantity);
        params.put("DISCOUNT", discount);
        params.put("TAX", tax);
        params.put("TOTAL_PRICE", String.valueOf(subTotal));
        params.put("ADDRESS", address);
        params.put("SHIPPING", shipping);
        params.put("PAYMENT", payment_method);
        params.put("ORDER_LIST", order_list);

        GsonRequest<SuccessObject> serverRequest = new GsonRequest<SuccessObject>(
                com.android.volley.Request.Method.POST,
                Helper.PATH_TO_PLACE_ORDER,
                SuccessObject.class,
                params,
                createOrderRequestSuccessListener(),
                createOrderRequestErrorListener());

        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(MpesaConfrimation.this).addToRequestQueue(serverRequest);
    }

    private Response.Listener<SuccessObject> createOrderRequestSuccessListener() {
        return new Response.Listener<SuccessObject>() {
            @Override
            public void onResponse(SuccessObject response) {
                try {
                    //Log.d(TAG, "Json esponse " + response.getSuccess());
                    if(response.getSuccess() == 1){
                        //delete paid other
                        ((CustomApplication)getApplication()).getShared().updateCartItems("");
                        //confirmation page.
                        Intent orderIntent = new Intent(MpesaConfrimation.this, ComfirmationActivity.class);
                        startActivity(orderIntent);
                    }else{
                        Helper.displayErrorMessage(MpesaConfrimation.this, "There is an issue why placing your order. Please try again");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener createOrderRequestErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                error.printStackTrace();
            }
        };
    }


}