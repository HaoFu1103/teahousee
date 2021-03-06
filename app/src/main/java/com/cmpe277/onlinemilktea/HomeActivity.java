package com.cmpe277.onlinemilktea;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.andremion.counterfab.CounterFab;
import com.cmpe277.onlinemilktea.Common.Common;
import com.cmpe277.onlinemilktea.Database.CartDataSource;
import com.cmpe277.onlinemilktea.Database.CartDatabase;
import com.cmpe277.onlinemilktea.Database.LocalCartDataSource;
import com.cmpe277.onlinemilktea.EventBus.BestDealItemClick;
import com.cmpe277.onlinemilktea.EventBus.CategoryClick;
import com.cmpe277.onlinemilktea.EventBus.CounterCartEvent;
import com.cmpe277.onlinemilktea.EventBus.FoodItemClick;
import com.cmpe277.onlinemilktea.EventBus.HideFabCart;
import com.cmpe277.onlinemilktea.EventBus.PopularCategoryClick;
import com.cmpe277.onlinemilktea.EventBus.UpdateItemInCart;
import com.cmpe277.onlinemilktea.Model.CategoryModel;
import com.cmpe277.onlinemilktea.Model.FoodModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
// implements NavigationView.OnNavigationItemSelectedListener
public class HomeActivity extends AppCompatActivity  {

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private CartDataSource cartDataSource;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    android.app.AlertDialog dialog;
    @BindView(R.id.fab)
    CounterFab fab;


    @Override
    protected void onResume() {
        super.onResume();
        countCartItem();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());

        // go to the cart detail page
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                navController.navigate(R.id.nav_cart);
            }
        });
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail, R.id.nav_cart,
                R.id.view_order)
                .setDrawerLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        countCartItem();

        navigationView.bringToFront();
        navigationView.getMenu().findItem(R.id.nav_signout).setOnMenuItemClickListener(menuItem -> {
           signout();
           return true;
        });

        navigationView.getMenu().findItem(R.id.map_house).setOnMenuItemClickListener(menuItem -> {
            navController.navigate(R.id.map_view);
            return true;
        });
        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = (TextView) headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hey, ", Common.currentUser.getName(), txt_user);

        TextView txt_email = (TextView) headerView.findViewById(R.id.txt_email);
        Common.setSpanString("Your email: ", Common.currentUser.getEmail(), txt_email);
    }

    private void signout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Signout")
                .setMessage("Do you really want to sign out?")
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Common.selectedFood = null;
                Common.categorySelected = null;
                Common.currentUser = null;
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public boolean onNavigateUp() {
        return super.onNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register( this );
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister( this );
        super.onStop();
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onHideFabEvent(HideFabCart event) {
        if (event.isHidden()) {
            fab.hide();
        } else {
            fab.show();
        }
    }



    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onCategorySelected(CategoryClick event) {
        if(event.isSuccess())
        {
            navController.navigate(R.id.nav_food_list);

            Toast.makeText( this, "Click to "+event.getCategoryModel().getName(), Toast.LENGTH_SHORT ).show();
        }
    }


    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onFoodItemClick(FoodItemClick event) {
        if(event.isSuccess())
        {
            navController.navigate(R.id.nav_food_detail);

        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onCartClick(UpdateItemInCart event)
    {
        if(event.isSuccess())
        {
            navController.navigate(R.id.nav_cart);

        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onCartCounter(CounterCartEvent event) {
        if(event.isSuccess())
        {
            countCartItem();

        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPopularItemClick(PopularCategoryClick event) {
        if (event.getPopularCategoryModel() != null) {
            dialog.show();

            FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.getPopularCategoryModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Common.categorySelected = dataSnapshot.getValue(CategoryModel.class);
                                Common.categorySelected.setMenu_id(dataSnapshot.getKey());

                                // Load food
                                FirebaseDatabase.getInstance()
                                        .getReference("Category")
                                        .child(event.getPopularCategoryModel().getMenu_id())
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.getPopularCategoryModel().getFood_id())
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                                                        Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                        Common.selectedFood.setKey(itemSnapShot.getKey());
                                                    }

                                                    navController.navigate(R.id.nav_food_detail);
                                                } else {

                                                    Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();
                                                }
                                                dialog.dismiss();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                dialog.dismiss();
                                                Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                dialog.dismiss();
                                Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onBestDealItemClick(BestDealItemClick event) {
        if (event.getBestDealModel() != null) {
            dialog.show();

            FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.getBestDealModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Common.categorySelected = dataSnapshot.getValue(CategoryModel.class);
                                Common.categorySelected.setMenu_id(dataSnapshot.getKey());

                                // Load food
                                FirebaseDatabase.getInstance()
                                        .getReference("Category")
                                        .child(event.getBestDealModel().getMenu_id())
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.getBestDealModel().getFood_id())
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                                                        Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                        Common.selectedFood.setKey(itemSnapShot.getKey());

                                                    }

                                                    navController.navigate(R.id.nav_food_detail);
                                                } else {

                                                    Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();
                                                }
                                                dialog.dismiss();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                dialog.dismiss();
                                                Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                dialog.dismiss();
                                Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void countCartItem() {
        cartDataSource.countItemCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull Integer integer) {
                        fab.setCount(integer);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
//                        if (!e.getMessage().contains("Query returned empty"))
//                           Toast.makeText(HomeActivity.this, "[COUNT CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
//                        else
                            fab.setCount(0);
                    }
                });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
