package com.demons.album

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.demons.album.MainAdapter.MainVH
import com.demons.media.models.album.entity.Photo

/**
 * 返回图片的列表适配器
 */
class MainAdapter internal constructor(cxt: Context?, private val list: ArrayList<Photo?>) :
    RecyclerView.Adapter<MainVH>() {
    private val mInflater: LayoutInflater
    private val mGlide: RequestManager

    init {
        mInflater = LayoutInflater.from(cxt)
        mGlide = Glide.with(cxt!!)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainVH {
        return MainVH(mInflater.inflate(R.layout.item, parent, false))
    }

    override fun onBindViewHolder(holder: MainVH, position: Int) {
        val photo = list[position]
        mGlide.load(photo!!.uri).into(holder.ivPhoto)
        holder.tvMessage.text =
            """
            [图片名称]： ${photo.name}
            [宽]：${photo.width}
            [高]：${photo.height}
            [文件大小,单位bytes]：${photo.size}
            [日期，时间戳，毫秒]：${photo.time}
            [图片地址]：${photo.path}
            [图片类型]：${photo.type}
            [是否选择原图]：${photo.selectedOriginal}
            [视频时长]：${photo.duration}
            """.trimIndent()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MainVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivPhoto: ImageView
        var tvMessage: TextView

        init {
            ivPhoto = itemView.findViewById<View>(R.id.iv_photo) as ImageView
            tvMessage = itemView.findViewById<View>(R.id.tv_message) as TextView
        }
    }
}