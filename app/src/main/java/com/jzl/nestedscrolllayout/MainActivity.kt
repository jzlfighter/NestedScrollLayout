package com.jzl.nestedscrolllayout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main1.bottomSheet
import kotlinx.android.synthetic.main.activity_main1.list

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)
        bottomSheet.elevation  = 15f
        initList()
    }

    private fun initList() {
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
    }

    private val adapter = object: RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.item_list, parent, false)
            )
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        }

        override fun getItemCount(): Int {
            return 20
        }

    }

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    }
}
