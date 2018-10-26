package com.tonney.shop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonObject;
import com.tonney.shop.adapter.CheckoutAdapter;
import com.tonney.shop.entity.CartObject;
import com.tonney.shop.entity.CheckoutObject;
import com.tonney.shop.entity.PaymentResponseObject;
import com.tonney.shop.entity.SuccessObject;
import com.tonney.shop.entity.UserObject;
import com.tonney.shop.mpesa.ApiConstants;
import com.tonney.shop.mpesa.MResponse;
import com.tonney.shop.mpesa.NotificationUtils;
import com.tonney.shop.mpesa.OAuth;
import com.tonney.shop.mpesa.STKPush;
import com.tonney.shop.mpesa.SharedPrefsUtil;
import com.tonney.shop.mpesa.Utils;
import com.tonney.shop.network.GsonRequest;
import com.tonney.shop.network.VolleySingleton;
import com.tonney.shop.utils.CustomApplication;
import com.tonney.shop.utils.CustomSharedPreference;
import com.tonney.shop.utils.Helper;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.tonney.shop.R.string.subtotal;
import static com.tonney.shop.mpesa.ApiConstants.PUSH_NOTIFICATION;
import static com.tonney.shop.mpesa.ApiConstants.REGISTRATION_COMPLETE;
import static com.tonney.shop.mpesa.ApiConstants.TOPIC_GLOBAL;
import static com.tonney.shop.utils.Helper.MY_SOCKET_TIMEOUT_MS;

public class CheckoutActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {


    int productDiscount = 0;


    String paymentSelected;


    private static final String TAG = CheckoutActivity.class.getSimpleName();

    private TextView orderItemCount, orderTotalAmount, orderVat, orderFullAmount, orderDiscount, orderShipping;
    private TextView restaurantName, restaurantAddress;

    private ImageView editAddress;

    private CustomSharedPreference shared;

    private RadioButton mainAdress, secondaryAddress;
    private RadioGroup addressGroup;
    private String alternativeAddress;
    private String selectedAddress = "";
    private static ProgressDialog d;
    private static String Numb;

    private RadioGroup radioGroup;
    private RadioButton payPalPayment;
    private RadioButton creditCardPayment;
    private RadioButton cashOnDelivery, lipaNaMpesa;

    private UserObject user;
    private List<CartObject> checkoutOrder;
    private String finalList;

    public static double subTotal;
    private double finalCost;

    private Gson gson;

    private CheckoutObject checkoutObject;
    private float taxPercent;

    private static PayPalConfiguration config;
    private static final int REQUEST_PAYMENT_CODE = 99;

    private boolean isDiscount = false;
    private String[] couponDiscountType;
    private int shippingCost;
    String payment = "";
    SweetAlertDialog pd;
    ////////////////////
    private String mFireBaseRegId;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private SharedPrefsUtil mSharedPrefsUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        getAccessToken();

        config = new PayPalConfiguration().environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK).clientId(Helper.PAY_PAL_CLIENT_ID);

        if(!Helper.isNetworkAvailable(this)){
            Helper.displayErrorMessage(this, getString(R.string.no_internet));
        }else{
            checkoutInformationFromRemoteServer();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTitle(getString(R.string.checkouts));
/////////////////
        mSharedPrefsUtil = new SharedPrefsUtil(this);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_GLOBAL);
                    getFirebaseRegId();

                } else if (intent.getAction().equals(PUSH_NOTIFICATION)) {
                    String message = intent.getStringExtra("message");
                    NotificationUtils.createNotification(getApplicationContext(), message);
                    showResultDialog(message);
                }
            }
        };

        getFirebaseRegId();
        ///////////////
        pd= new SweetAlertDialog(CheckoutActivity.this,SweetAlertDialog.PROGRESS_TYPE);
        d=new ProgressDialog(this);
        d.setCancelable(false);
        ///////////
        isDiscount = getIntent().getExtras().getBoolean("REDEEM");

        finalList = getIntent().getExtras().getString("FINAL_ORDER");
        Log.d(TAG, "JSON FORMAT" + finalList);
        gson = ((CustomApplication)getApplication()).getGsonObject();
        CartObject[] mOrders = gson.fromJson(finalList, CartObject[].class);
        checkoutOrder = ((CustomApplication)getApplication()).convertObjectArrayToListObject(mOrders);

        couponDiscountType = getProductDiscount(checkoutOrder);

        shared = ((CustomApplication)getApplication()).getShared();
        user = gson.fromJson(shared.getUserData(), UserObject.class);

        radioGroup = (RadioGroup)findViewById(R.id.radio_group);
        lipaNaMpesa = (RadioButton)findViewById(R.id.lipaNaMpesa);
        lipaNaMpesa.setOnCheckedChangeListener(this);




        editAddress = (ImageView)findViewById(R.id.add_address);

        addressGroup = (RadioGroup)findViewById(R.id.address_group);
        //selectDeliveryAddress();
        mainAdress = (RadioButton)findViewById(R.id.main_address);
        secondaryAddress = (RadioButton)findViewById(R.id.secondary_address);

        restaurantName = (TextView)findViewById(R.id.restaurant_name);
        restaurantAddress = (TextView)findViewById(R.id.restaurant_address);

        alternativeAddress = ((CustomApplication)getApplication()).getShared().getSavedDeliveryAddress();
        if(TextUtils.isEmpty(alternativeAddress)){
            secondaryAddress.setVisibility(View.GONE);
            mainAdress.setText(user.getAddress());
        }else{
            secondaryAddress.setText(alternativeAddress);
            mainAdress.setText(user.getAddress());
        }

        orderItemCount = (TextView)findViewById(R.id.order_item_count);
        orderTotalAmount =(TextView)findViewById(R.id.order_total_amount);
        orderVat = (TextView)findViewById(R.id.order_vat);
        orderFullAmount = (TextView)findViewById(R.id.order_full_amounts);

        orderDiscount = (TextView)findViewById(R.id.order_discount);
        if(isDiscount){
            if(couponDiscountType[0].equals("Percentage")){
                orderDiscount.setText(couponDiscountType[1] + "%");
            }else{
                orderDiscount.setText("Ksh" + couponDiscountType[1]);
            }
        }
        orderShipping = (TextView)findViewById(R.id.order_shipping);

        orderItemCount.setText(String.valueOf(checkoutOrder.size()));
        subTotal = ((CustomApplication)getApplication()).getSubtotalAmount(checkoutOrder);
        orderTotalAmount.setText("Ksh" + String.valueOf(subTotal) + "0");

        RecyclerView checkoutRecyclerView = (RecyclerView)findViewById(R.id.checkout_item);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        checkoutRecyclerView.setLayoutManager(linearLayoutManager);

        checkoutRecyclerView.setHasFixedSize(true);

        CheckoutAdapter mAdapter = new CheckoutAdapter(CheckoutActivity.this, checkoutOrder);
        checkoutRecyclerView.setAdapter(mAdapter);

        editAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newAddressIntent = new Intent(CheckoutActivity.this, NewAddressActivity.class);
                newAddressIntent.putExtra("PRIMARY_ADDRESS", user.getAddress());
                startActivity(newAddressIntent);
            }
        });

        Button placeOrderButton = (Button)findViewById(R.id.place_an_order);
        placeOrderButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View view) {
//                if(radioGroup.getCheckedRadioButtonId() < 0){
//                    Helper.displayErrorMessage(CheckoutActivity.this, "Payment option must be selected before checkout");
//                    return;
//                }

                if(addressGroup.getCheckedRadioButtonId() < 0){
                    Helper.displayErrorMessage(CheckoutActivity.this, "You must provide a delivery address before checkout");
                    return;
                }




                String sAddress = returnAddress();
                Log.d(TAG, "Address: " + sAddress);

                paymentSelected = payment;



                Log.d(TAG, "Payment: " + paymentSelected);


                if(isDiscount){
                    productDiscount = Integer.parseInt(couponDiscountType[1]);
                }

                if(paymentSelected.equals("PAY PAL")){
                    initializePayPalPayment();
                    Log.d(TAG, "Still inside");
                }else if(paymentSelected.equals("CREDIT CARD")){
                    String serverContent = user.getId() + ";" + checkoutOrder.size() + ";" + productDiscount + ";" + taxPercent +
                            ";" + finalCost + ";" + sAddress + ";" + shippingCost + ";" + paymentSelected + ";" + finalList;
                    Intent cardIntent = new Intent(CheckoutActivity.this, CreditCardActivity.class);
                    Log.d(TAG, "Server Content " + serverContent);
                    cardIntent.putExtra("STRIPE_PAYMENT", serverContent);
                    startActivity(cardIntent);
                }else if(paymentSelected.equals("CASH ON DELIVERY")){
                    postCheckoutOrderToRemoteServer(String.valueOf(user.getId()), String.valueOf(checkoutOrder.size()), String.valueOf(productDiscount),
                            String.valueOf(taxPercent), String.valueOf(finalCost), sAddress, String.valueOf(shippingCost), paymentSelected, finalList);
                }else if(paymentSelected.equalsIgnoreCase("LIPA NA MPESA")){
                    String serverContent = user.getId() + ";" + checkoutOrder.size() + ";" + productDiscount + ";" + taxPercent +
                            ";" + finalCost + ";" + sAddress + ";" + shippingCost + ";" + paymentSelected + ";" + finalList;
                    Intent mpesaIntent = new Intent(CheckoutActivity.this, MpesaConfrimation.class);
                    Log.d(TAG, "Server Content " + serverContent);
                    mpesaIntent.putExtra("MPESA_PAYMENT", serverContent);
                    startActivity(mpesaIntent);

                }
            }
        });

    }

    private void getAccessToken() {

    }


    private void getFirebaseRegId() {
        mFireBaseRegId = mSharedPrefsUtil.getFirebaseRegistrationID();

        if (!TextUtils.isEmpty(mFireBaseRegId)) {
            mSharedPrefsUtil.saveFirebaseRegistrationID(mFireBaseRegId);
        }
    }

    private void showResultDialog(String message) {
        if (!mSharedPrefsUtil.getIsFirstTime()) {
            // run your one time code
            mSharedPrefsUtil.saveIsFirstTime(true);

            new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Success")
                    .setContentText("Checkout Processed successfuly")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            sweetAlertDialog.dismissWithAnimation();
                            mSharedPrefsUtil.saveIsFirstTime(false);
                        }
                    })
                    .show();
        }
    }

    private void showMpesaPopup() {
        final Dialog d = new Dialog(CheckoutActivity.this);
        d.setContentView(R.layout.mpesa_popup);
        d.setCancelable(false);
        final TextView subTotal = d.findViewById(R.id.totalCostTV);
        subTotal.setText(String.valueOf(finalCost).split("\\.")[0]);
        d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //  d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //   d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final TextInputEditText phone = d.findViewById(R.id.mpesa_phone_number);

        ImageView cancel = d.findViewById(R.id.cancelMpesaPopup);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                d.dismiss();
            }
        });
        Button pay = d.findViewById(R.id.mpesaPayBTN);
        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Numb= phone.getText().toString();
                if (!Numb.isEmpty()
                        && !String.valueOf(subTotal).equals("")
                        && Utils.isNetworkAvailable(CheckoutActivity.this))
                {
                    d.dismiss();

                    OAuth oAuth = new OAuth(
                            ApiConstants.safaricom_Auth_key,
                            ApiConstants.safaricom_Secret);
                    oAuth.setProduction(ApiConstants.PRODUCTION_DEBUG);


                    String url = ApiConstants.BASE_URL + ApiConstants.ACCESS_TOKEN_URL;

                    if (oAuth.getProduction() == ApiConstants.PRODUCTION_RELEASE)
                        url = ApiConstants.PRODUCTION_BASE_URL + ApiConstants.ACCESS_TOKEN_URL;


                    new AuthService(CheckoutActivity.this).execute(url, oAuth.getOauth());


                } else {
                    Toast.makeText(CheckoutActivity.this, "An Error Occurred, Fill required fields and have internet on", Toast.LENGTH_LONG).show();
                }


            }
        });
        d.show();

    }
    class AuthService extends AsyncTask<String, Void, String> {

        final Context context;

        AuthService(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            d.setMessage("Processing Request...");
            d.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Basic " + strings[1]);
            return com.tonney.shop.mpesa.Request.get(strings[0], headers);
        }

        protected void onPostExecute(String result) {
            d.setMessage("Finishing up...");
            if (result == null) {
                Toast.makeText(context, "Error Getting Oauth Key", Toast.LENGTH_LONG).show();
                d.dismiss();
                return;
            }

            try {

                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.get("access_token") != null) {

                    String token = jsonObject.get("access_token").toString();

                    String mpesaAmount = String.valueOf(finalCost);

                    STKPush stkPush = new
                            STKPush(
                            ApiConstants.safaricom_bussiness_short_code,
                            ApiConstants.DEFAULT_TRANSACTION_TYPE,
                            //TODO: eka hapa subtotal,
                            String.valueOf(String.valueOf(finalCost).split("\\.")[0]),
                            Utils.sanitizePhoneNumber(Numb),
                            ApiConstants.safaricom_party_b,
                            Utils.sanitizePhoneNumber(Numb),
                            ApiConstants.callback_url,
                            "Shop",
                            "Pay");


                    String url = ApiConstants.BASE_URL + ApiConstants.PROCESS_REQUEST_URL;

                    if (stkPush.getProduction() == ApiConstants.PRODUCTION_RELEASE) {
                        url = ApiConstants.PRODUCTION_BASE_URL + ApiConstants.PROCESS_REQUEST_URL;
                    }


                    new PayService().execute(url, generateJsonStringParams(stkPush), token);

                }

                return;
            } catch (Exception ignored) {


            }
        }
    }

    class PayService extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + strings[2]);
            return com.tonney.shop.mpesa.Request.post(strings[0], strings[1], headers);

        }

        protected void onPostExecute(String result) {
            /*

            {
                "MerchantRequestID":"11273-1846699-2",
                "CheckoutRequestID":"ws_CO_DMZ_59302413_05082018193602819",
                "ResponseCode": "0",
                "ResponseDescription":"Success. Request accepted for processing",
                "CustomerMessage":"Success. Request accepted for processing"
                    }
          */

            d.dismiss();
            // Toast.makeText(CheckoutActivity.this, result, Toast.LENGTH_SHORT).show();
            //  Log.d("RES::",result);


            try {
                JSONObject object = new JSONObject(result);

                String responseDescription = object.getString("ResponseDescription");
                String ResponseCode = object.getString("ResponseCode");
                //todo: encrypt before saving to statics
                MResponse.CheckoutRequestID= object.getString("CheckoutRequestID");
                //   Toast.makeText(CheckoutActivity.this, MResponse.CheckoutRequestID, Toast.LENGTH_SHORT).show();
                if(ResponseCode.equals("0") && responseDescription.contains("Success. Request accepted for processing")){
                    //it opened sim toolkit so tutume resp kwa callback url to see if he actually paid

                    pd.setTitleText("Almost Done");
                    pd.setContentText("Contacting servers ...");
                    pd.show();
                    ApiConstants.isFromstk=true;

                }else{
                    //some other error for now istead of querying callback just an error resp will do
                    //TODO:save history
                    Toasty.error(CheckoutActivity.this,"Something went wrong.. Try again after some seconds",Toast.LENGTH_SHORT).show();

                }



            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    private void ParseResult(String response, SweetAlertDialog pd) throws JSONException {
        SweetAlertDialog pdd = new SweetAlertDialog(CheckoutActivity.this,SweetAlertDialog.PROGRESS_TYPE);
        Log.d("CCCC: ",response);
        JSONObject jsonObject= new JSONObject(response);
        JSONObject body = jsonObject.getJSONObject("Body");
        JSONObject stkCallback= body.getJSONObject("stkCallback");
        String CODE= stkCallback.getString("ResultCode");
        //Toasty.error(CheckoutActivity.this,CODE,Toast.LENGTH_LONG).show();
        if(CODE.trim().equals("0")){



            pdd
                    .setTitleText("Order Successfull!")
                    .setContentText("")
                    .setConfirmText("OK")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            sweetAlertDialog.dismiss();
                            finish();
                        }
                    })
                    .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
            pdd.show();

            postCheckoutOrderToRemoteServer(String.valueOf(user.getId()), String.valueOf(checkoutOrder.size()), String.valueOf(productDiscount),
                    String.valueOf(taxPercent), String.valueOf(finalCost), returnAddress(), String.valueOf(shippingCost), paymentSelected, finalList);

            pdd
                    .setTitleText("Order UnSuccessfull!")
                    .setContentText("Kindly, Try again after some time")
                    .setConfirmText("OK")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            sweetAlertDialog.dismiss();
                            //finish();
                        }
                    })
                    .changeAlertType(SweetAlertDialog.ERROR_TYPE);
            pdd.show();
        }

    }

    private static String generateJsonStringParams(STKPush push) {
        JSONObject postData = new JSONObject();

        try {
            postData.put("BusinessShortCode", push.getBusinessShortCode());
            postData.put("Password", push.getPassword());
            postData.put("Timestamp", Utils.getTimestamp());
            postData.put("TransactionType", push.getTransactionType());
            postData.put("Amount", push.getAmount());
            postData.put("PartyA", push.getPartyA());
            postData.put("PartyB", push.getPartyB());
            postData.put("PhoneNumber", push.getPhoneNumber());
            postData.put("CallBackURL", push.getCallBackURL());
            postData.put("AccountReference", push.getAccountReference());
            postData.put("TransactionDesc", push.getTransactionDesc());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return postData.toString();

    }
    private void checkoutInformationFromRemoteServer(){
        GsonRequest<CheckoutObject> serverRequest = new GsonRequest<CheckoutObject>(
                Request.Method.GET,
                Helper.PATH_TO_CHECKOUT,
                CheckoutObject.class,
                createRequestSuccessListener(),
                createRequestErrorListener());

        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(this).addToRequestQueue(serverRequest);
    }

    private Response.Listener<CheckoutObject> createRequestSuccessListener() {
        return new Response.Listener<CheckoutObject>() {
            @Override
            public void onResponse(CheckoutObject response) {
                try {
                    Log.d(TAG, "Json Response " + response.toString());
                    if(!TextUtils.isEmpty(response.getCompany_name()) || !TextUtils.isEmpty(response.getCompany_address())){
                        checkoutObject = response;
                        restaurantName.setText(response.getCompany_name());
                        restaurantAddress.setText(response.getCompany_address());

                        shippingCost = response.getSettings_shipping();
                        taxPercent = response.getSettings_tax();

                        orderShipping.setText("Ksh" + shippingCost);

                        finalCost = getFinalTotalPrice(shippingCost, taxPercent);

                        orderVat.setText(String.valueOf(taxPercent) + "%");
                        orderFullAmount.setText("Ksh" + String.valueOf(finalCost));

                    }else{
                        Helper.displayErrorMessage(CheckoutActivity.this, "Shop information missing in the server! please contact admin");
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


    private double getFinalTotalPrice(int shipping, float tax){
        if(isDiscount){
            int discount = Integer.parseInt(couponDiscountType[1]);
            subTotal = subTotal - discount;
        }

        int shippingAmount = 0;
        double discountTotal = 0;
        if(shipping != 0){
            shippingAmount = shipping;
        }
        discountTotal = subTotal + shippingAmount;
        Log.d(TAG, "subtotal " + subtotal);
        Log.d(TAG, "discount " + discountTotal);
        int taxAmount = 0;
        if(tax != 0){
            taxAmount = (int)((tax * discountTotal)/100);
        }
        double finalTotal = discountTotal + taxAmount;
        return finalTotal;
    }

    private String returnAddress(){
        String chosenAddress = "";
        if(mainAdress.isChecked()){
            chosenAddress = user.getAddress();
        }
        if(secondaryAddress.isChecked()){
            chosenAddress = alternativeAddress;
        }
        return chosenAddress;
    }

    private String getSelectedPayment(){
        if(cashOnDelivery.isChecked()){
            payment = "LIPA NA MPESA";
        }
        return payment;
    }

    private void initializePayPalPayment(){
        PayPalPayment payment = new PayPalPayment(new BigDecimal(String.valueOf(subTotal)), "USD", "Product Order", PayPalPayment.PAYMENT_INTENT_SALE);
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
        startActivityForResult(intent, REQUEST_PAYMENT_CODE);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PAYMENT_CODE) {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
                try {
                    String jsonPaymentResponse = confirm.toJSONObject().toString(4);
                    GsonBuilder builder = new GsonBuilder();
                    Gson gson = builder.create();
                    PaymentResponseObject responseObject = gson.fromJson(jsonPaymentResponse, PaymentResponseObject.class);
                    if(responseObject != null){
                        String paymentId = responseObject.getResponse().getId();
                        String paymentState = responseObject.getResponse().getState();
                        Log.d(TAG, "Log payment id and state " + paymentId + " " + paymentState);

                        int productDiscount = 0;
                        if(isDiscount){
                            productDiscount = Integer.parseInt(couponDiscountType[1]);
                        }
                        //send order to server
                        String deliveryAddress = returnAddress();
                        postCheckoutOrderToRemoteServer(String.valueOf(user.getId()), String.valueOf(checkoutOrder.size()), String.valueOf(productDiscount),
                                String.valueOf(taxPercent), String.valueOf(finalCost), deliveryAddress, String.valueOf(shippingCost), "PAY PAL", finalList);

                    }
                } catch (JSONException e) {
                    Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
                }
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            Log.i("paymentExample", "The user canceled.");
        }
        else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            Log.i("paymentExample", "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
        }
    }


    private void postCheckoutOrderToRemoteServer(String userId, String quantity, String discount, String tax, String price,
                                                 String address, String shipping, String payment_method, String order_list){
        Map<String, String> params = new HashMap<String,String>();
        params.put("USER_ID", userId);
        params.put("QUANTITY", quantity);
        params.put("DISCOUNT", discount);
        params.put("TAX", tax);
        params.put("TOTAL_PRICE", price);
        params.put("ADDRESS", address);
        params.put("SHIPPING", shipping);
        params.put("PAYMENT", payment_method);
        params.put("ORDER_LIST", order_list);

        GsonRequest<SuccessObject> serverRequest = new GsonRequest<SuccessObject>(
                Request.Method.POST,
                Helper.PATH_TO_PLACE_ORDER,
                SuccessObject.class,
                params,
                createOrderRequestSuccessListener(),
                createOrderRequestErrorListener());

        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(CheckoutActivity.this).addToRequestQueue(serverRequest);
    }

    private Response.Listener<SuccessObject> createOrderRequestSuccessListener() {
        return new Response.Listener<SuccessObject>() {
            @Override
            public void onResponse(SuccessObject response) {
                try {
                    Log.d(TAG, "Json Response " + response.getSuccess());
                    if(response.getSuccess() == 1){
                        //delete paid other
                        ((CustomApplication)getApplication()).getShared().updateCartItems("");
                        //confirmation page.
                        Intent orderIntent = new Intent(CheckoutActivity.this, ComfirmationActivity.class);
                        startActivity(orderIntent);
                    }else{
                        Helper.displayErrorMessage(CheckoutActivity.this, "There is an issue why placing your order. Please try again");
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

    private String[] getProductDiscount(List<CartObject> productsInCart){
        String[] cType = new String[2];
        for(int i = 0; i < productsInCart.size(); i++){
            CartObject productObject = productsInCart.get(i);
            if(!TextUtils.isEmpty(productObject.getCouponType())){
                String discountType = productObject.getCouponType();
                int discountValue = productObject.getDiscount();
                cType[0] = discountType;
                cType[1] = String.valueOf(discountValue);
                break;
            }
        }
        return cType;
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ApiConstants.isFromstk == true) {
            pd.setTitleText("Almost Done");
            pd.setContentText("Awaiting mpesa response ...");


            final StringRequest stringRequest = new StringRequest(Request.Method.POST,
                    ApiConstants.CHECH_CALLBACK, new Response.Listener<String>() {
                @Override
                public void onResponse(final String response) {
                    pd.dismissWithAnimation();
                    ApiConstants.isFromstk = false;

                    //   Toasty.error(CheckoutActivity.this,response, Toast.LENGTH_LONG).show();
                    Log.d("XXXX", response);


                    try {
                        ParseResult(response, pd);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    pd.dismissWithAnimation();
                    ApiConstants.isFromstk = false;
                    Log.d("RESPONS:: ", error.toString());

                    //Toasty.error(PostMerchantDetails.this,error.toString(), Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String, String> data = new HashMap<>();
                    data.put("id", MResponse.CheckoutRequestID);
                    return data;

                }
            };

            stringRequest.setRetryPolicy(new RetryPolicy() {
                @Override
                public int getCurrentTimeout() {
                    return 5000;
                }

                @Override
                public int getCurrentRetryCount() {
                    return 1;
                }

                @Override
                public void retry(VolleyError error) throws VolleyError {

                }
            });
            final RequestQueue queue = Volley.newRequestQueue(CheckoutActivity.this);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    queue.add(stringRequest);

                }
            },10000);



        }
        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


        switch (compoundButton.getId()){

            case R.id.lipaNaMpesa:
                if(b){
                    payment = "LIPA NA MPESA";

                }
                break;




        }
    }
}
