package com.demons.media.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.demons.media.R
import com.demons.media.constant.Type
import com.demons.media.models.ad.AdViewHolder
import com.demons.media.models.album.entity.Photo
import com.demons.media.result.Result
import com.demons.media.setting.Setting
import com.demons.media.ui.widget.PressedImageView
import com.demons.media.utils.ToastUtil
import com.demons.media.utils.media.DurationUtils
import java.lang.ref.WeakReference

/**
 * 专辑相册适配器
 */
class PhotosAdapter(
    private val cxt: Context,
    private val dataList: ArrayList<Any?>,
    private val listener: OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(cxt)
    private var unable: Boolean
    private val isSingle: Boolean
    private var singlePosition = 0
    private var clearAd = false

    init {
        unable = Result.count() == Setting.count
        isSingle = Setting.count == 1
    }

    fun change() {
        unable = Result.count() == Setting.count
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_AD -> return AdViewHolder(
                mInflater.inflate(
                    R.layout.item_ad_photos,
                    parent,
                    false
                )
            )
            TYPE_CAMERA -> return CameraViewHolder(
                mInflater.inflate(
                    R.layout.item_camera_photos,
                    parent,
                    false
                )
            )
            else -> return PhotoViewHolder(
                mInflater.inflate(
                    R.layout.item_rv_photos,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        if (holder is PhotoViewHolder) {
            val item: Photo = dataList[position] as Photo
            updateSelector(holder.tvSelector, item.selected, item, position)
            val path = item.path
            val uri = item.uri
            val type = item.type
            val duration = item.duration
            val isGif = path.endsWith(Type.GIF) || type.endsWith(Type.GIF)
            if (Setting.showGif && isGif) {
                Setting.imageEngine.loadGifAsBitmap(holder.ivPhoto.context, uri, holder.ivPhoto)
                holder.tvType.setText(R.string.gif_easy_photos)
                holder.tvType.visibility = View.VISIBLE
                holder.ivVideo.visibility = View.GONE
            } else if (Setting.showVideo && type.contains(Type.VIDEO)) {
                Setting.imageEngine.loadPhoto(holder.ivPhoto.context, uri, holder.ivPhoto)
                holder.tvType.text = DurationUtils.format(duration)
                holder.tvType.visibility = View.VISIBLE
                holder.ivVideo.visibility = View.VISIBLE
            } else {
                Setting.imageEngine.loadPhoto(holder.ivPhoto.context, uri, holder.ivPhoto)
                holder.tvType.visibility = View.GONE
                holder.ivVideo.visibility = View.GONE
            }
            holder.vSelector.visibility = View.VISIBLE
            holder.tvSelector.visibility = View.VISIBLE
            holder.ivPhoto.setOnClickListener {
                var realPosition = position
                if (Setting.hasPhotosAd()) {
                    realPosition--
                }
                if (Setting.isShowCamera && !Setting.isBottomRightCamera()) {
                    realPosition--
                }
                listener.onPhotoClick(position, realPosition)
            }
            holder.vSelector.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    if (isSingle) {
                        singleSelector(item, position)
                        return
                    }
                    if (unable) {
                        if (item.selected) {
                            Result.removePhoto(item)
                            if (unable) {
                                unable = false
                            }
                            listener.onSelectorChanged()
                            notifyDataSetChanged()
                            return
                        }
                        listener.onSelectorOutOfMax(null)
                        return
                    }
                    item.selected = !item.selected
                    if (item.selected) {
                        if (item.type.contains(Type.VIDEO) && item.size > Setting.videoMaxSize) {
                            ToastUtil.show(cxt, R.string.selector_video_max_size)
                            item.selected = false
                            return
                        }
                        if (!item.type.contains(Type.VIDEO) && item.size > Setting.photoMaxSize) {
                            ToastUtil.show(cxt, R.string.selector_photo_max_size)
                            item.selected = false
                            return
                        }
                        val res = Result.addPhoto(item)
                        if (res != 0) {
                            listener.onSelectorOutOfMax(res)
                            item.selected = false
                            return
                        }
                        holder.tvSelector.setBackgroundResource(R.drawable.bg_select_true_easy_photos)
                        holder.tvSelector.text = Result.count().toString()
                        if (Result.count() == Setting.count) {
                            unable = true
                            notifyDataSetChanged()
                        }
                    } else {
                        Result.removePhoto(item)
                        if (unable) {
                            unable = false
                        }
                        notifyDataSetChanged()
                    }
                    listener.onSelectorChanged()
                }
            })
            return
        }
        if (holder is AdViewHolder) {
            if (clearAd) {
                holder.adFrame.removeAllViews()
                holder.adFrame.visibility = View.GONE
                return
            }
            if (!Setting.photoAdIsOk) {
                holder.adFrame.visibility = View.GONE
                return
            }
            val weakReference: WeakReference<Any> = dataList[position] as WeakReference<Any>
            val adView = weakReference.get() as View?
            if (null != adView) {
                if (null != adView.parent) {
                    if (adView.parent is FrameLayout) {
                        (adView.parent as FrameLayout).removeAllViews()
                    }
                }
                holder.adFrame.visibility = View.VISIBLE
                holder.adFrame.removeAllViews()
                holder.adFrame.addView(adView)
            }
        }
        if (holder is CameraViewHolder) {
            holder.flCamera.setOnClickListener { listener.onCameraClick() }
        }
    }

    fun clearAd() {
        clearAd = true
        notifyDataSetChanged()
    }

    private fun singleSelector(photo: Photo, position: Int) {
        if (!Result.isEmpty()) {
            if ((Result.getPhotoPath(0) == photo.path)) {
                Result.removePhoto(photo)
            } else {
                Result.removePhoto(0)
                Result.addPhoto(photo)
                notifyItemChanged(singlePosition)
            }
        } else {
            Result.addPhoto(photo)
        }
        notifyItemChanged(position)
        listener.onSelectorChanged()
    }

    private fun updateSelector(
        tvSelector: TextView,
        selected: Boolean,
        photo: Photo,
        position: Int
    ) {
        if (selected) {
            val number = Result.getSelectorNumber(photo)
            if ((number == "0")) {
                tvSelector.setBackgroundResource(R.drawable.bg_select_false_easy_photos)
                tvSelector.text = null
                return
            }
            tvSelector.text = number
            tvSelector.setBackgroundResource(R.drawable.bg_select_true_easy_photos)
            if (isSingle) {
                singlePosition = position
                tvSelector.text = "1"
            }
        } else {
            if (unable) {
                tvSelector.setBackgroundResource(R.drawable.bg_select_false_unable_easy_photos)
            } else {
                tvSelector.setBackgroundResource(R.drawable.bg_select_false_easy_photos)
            }
            tvSelector.text = null
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun getItemViewType(position: Int): Int {
        if (0 == position) {
            if (Setting.hasPhotosAd()) {
                return TYPE_AD
            }
            if (Setting.isShowCamera && !Setting.isBottomRightCamera()) {
                return TYPE_CAMERA
            }
        }
        if (1 == position && !Setting.isBottomRightCamera()) {
            if (Setting.hasPhotosAd() && Setting.isShowCamera) {
                return TYPE_CAMERA
            }
        }
        return TYPE_ALBUM_ITEMS
    }

    interface OnClickListener {
        fun onCameraClick()
        fun onPhotoClick(position: Int, realPosition: Int)
        fun onSelectorOutOfMax(result: Int?)
        fun onSelectorChanged()
    }

    private class CameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val flCamera: FrameLayout

        init {
            flCamera = itemView.findViewById(R.id.fl_camera)
        }
    }

    class PhotoViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: PressedImageView
        val tvSelector: TextView
        val vSelector: View
        val tvType: TextView
        val ivVideo: ImageView

        init {
            ivPhoto = itemView.findViewById(R.id.iv_photo)
            tvSelector = itemView.findViewById(R.id.tv_selector)
            vSelector = itemView.findViewById(R.id.v_selector)
            tvType = itemView.findViewById(R.id.tv_type)
            ivVideo = itemView.findViewById(R.id.iv_play)
        }
    }

    companion object {
        private const val TYPE_AD = 0
        private const val TYPE_CAMERA = 1
        private const val TYPE_ALBUM_ITEMS = 2
    }
}