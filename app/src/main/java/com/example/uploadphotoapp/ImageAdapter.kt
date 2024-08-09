package com.example.uploadphotoapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide

class ImageAdapter(private val context: Context, private val imageUrls: List<String>) : BaseAdapter() {

    override fun getCount(): Int = imageUrls.size

    override fun getItem(position: Int): Any = imageUrls[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false)
        val imageView: ImageView = view.findViewById(R.id.imageView)

        Glide.with(context)
            .load(imageUrls[position])
            .into(imageView)

        return view
    }
}
