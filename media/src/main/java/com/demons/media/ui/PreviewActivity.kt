package com.demons.media.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.demons.media.R
import com.demons.media.constant.Code
import com.demons.media.constant.Key
import com.demons.media.constant.Type
import com.demons.media.models.album.AlbumModel
import com.demons.media.models.album.entity.Photo
import com.demons.media.result.Result
import com.demons.media.setting.Setting
import com.demons.media.ui.PreviewFragment.OnPreviewFragmentClickListener
import com.demons.media.ui.adapter.PreviewPhotosAdapter
import com.demons.media.ui.dialog.MediaConfirmDialog
import com.demons.media.ui.widget.PressedTextView
import com.demons.media.utils.Color.ColorUtils
import com.demons.media.utils.file.FileUtils
import com.demons.media.utils.system.SystemUtils

/**
 * 预览页
 */
class PreviewActivity : AppCompatActivity(), PreviewPhotosAdapter.OnClickListener,
    View.OnClickListener, OnPreviewFragmentClickListener {
    private val mHideHandler = Handler(Looper.getMainLooper())
    private val mHidePart2Runnable =
        Runnable { SystemUtils.getInstance().systemUiHide(this@PreviewActivity, decorView) }
    private var mBottomBar: ConstraintLayout? = null
    private var mToolBar: FrameLayout? = null
    private val mShowPart2Runnable = Runnable { // 延迟显示UI元素
        mBottomBar!!.visibility = View.VISIBLE
        mToolBar!!.visibility = View.VISIBLE
    }
    private var mVisible = false
    private var decorView: View? = null
    private var tvNumber: TextView? = null
    private var tvOriginal: AppCompatCheckBox? = null
    private var originalAllSize: TextView? = null
    private var tvDone: PressedTextView? = null
    private var ivSelector: ImageView? = null
    private var rvPhotos: RecyclerView? = null
    private var adapter: PreviewPhotosAdapter? = null
    private var snapHelper: PagerSnapHelper? = null
    private var lm: LinearLayoutManager? = null
    private var index = 0
    private val photos = ArrayList<Photo>()
    private var resultCode = RESULT_CANCELED
    private var lastPosition = 0 //记录recyclerView最后一次角标位置，用于判断是否转换了item
    private val isSingle = Setting.count == 1
    private var unable = Result.count() == Setting.count
    private var flFragment: FrameLayout? = null
    private var previewFragment: PreviewFragment? = null
    private var statusColor = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        decorView = window.decorView
        SystemUtils.getInstance().systemUiInit(this, decorView)
        setContentView(R.layout.activity_preview)
        hideActionBar()
        adaptationStatusBar()
        if (null == AlbumModel.instance) {
            finish()
            return
        }
        initData()
        initView()
    }

    private fun adaptationStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            statusColor = ContextCompat.getColor(this, R.color.photos_status_bar)
            if (ColorUtils.isWhiteColor(statusColor)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
        }
    }

    private fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    private fun initData() {
        val intent = intent
        val albumItemIndex = intent.getIntExtra(Key.PREVIEW_ALBUM_ITEM_INDEX, 0)
        photos.clear()
        if (albumItemIndex == -1) {
            photos.addAll(Result.photos)
        } else {
            photos.addAll(AlbumModel.instance.getCurrAlbumItemPhotos(albumItemIndex))
        }
        index = intent.getIntExtra(Key.PREVIEW_PHOTO_INDEX, 0)
        lastPosition = index
        mVisible = true
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        val hideAnimation = AlphaAnimation(1.0f, 0.0f)
        hideAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mBottomBar!!.visibility = View.GONE
                mToolBar!!.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        hideAnimation.duration = UI_ANIMATION_DELAY.toLong()
        mBottomBar!!.startAnimation(hideAnimation)
        mToolBar!!.startAnimation(hideAnimation)
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        SystemUtils.getInstance().systemUiShow(this, decorView)
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.post(mShowPart2Runnable)
    }

    override fun onPhotoClick() {
        toggle()
    }

    override fun onPhotoScaleChanged() {
        if (mVisible) hide()
    }

    override fun onBackPressed() {
        doBack()
    }

    private fun doBack() {
        val intent = Intent()
        intent.putExtra(Key.PREVIEW_CLICK_DONE, false)
        setResult(resultCode, intent)
        finish()
    }

    private fun initView() {
        setClick(R.id.iv_back, R.id.tv_edit, R.id.tv_selector)
        mToolBar = findViewById<View>(R.id.m_top_bar_layout) as FrameLayout
        if (!SystemUtils.getInstance().hasNavigationBar(this)) {
            val mRootView = findViewById<View>(R.id.m_root_view) as LinearLayoutCompat
            mRootView.fitsSystemWindows = true
            mToolBar!!.setPadding(0, SystemUtils.getInstance().getStatusBarHeight(this), 0, 0)
            if (ColorUtils.isWhiteColor(statusColor)) {
                SystemUtils.getInstance().setStatusDark(this, true)
            }
        }
        mBottomBar = findViewById<View>(R.id.m_bottom_bar) as ConstraintLayout
        ivSelector = findViewById<View>(R.id.iv_selector) as ImageView
        tvNumber = findViewById<View>(R.id.tv_number) as TextView
        tvDone = findViewById<View>(R.id.tv_done) as PressedTextView
        originalAllSize = findViewById<View>(R.id.original_all_size) as TextView
        tvOriginal = findViewById<View>(R.id.tv_original) as AppCompatCheckBox
        tvOriginal!!.setOnCheckedChangeListener { _, isChecked ->
            Setting.selectedOriginal = isChecked
            if (isChecked) {
                originalAllSize!!.visibility = View.VISIBLE
                calculateFileSize()
            } else {
                originalAllSize!!.visibility = View.INVISIBLE
            }
        }
        flFragment = findViewById<View>(R.id.fl_fragment) as FrameLayout
        previewFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_preview) as PreviewFragment?
        if (Setting.showOriginalMenu) {
            tvOriginal?.visibility = View.VISIBLE
            tvOriginal?.isChecked = Setting.selectedOriginal
            if ( Setting.selectedOriginal) {
                originalAllSize!!.visibility = View.VISIBLE
                calculateFileSize()
            } else {
                originalAllSize!!.visibility = View.INVISIBLE
            }
        } else {
            tvOriginal!!.visibility = View.GONE
        }
        calculateFileSize()
        setClick(tvDone!!, ivSelector!!)
        initRecyclerView()
        shouldShowMenuDone()
    }

    private fun initRecyclerView() {
        rvPhotos = findViewById<View>(R.id.rv_photos) as RecyclerView
        adapter = PreviewPhotosAdapter(this, photos, this)
        lm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos!!.layoutManager = lm
        rvPhotos!!.adapter = adapter
        rvPhotos!!.scrollToPosition(index)
        toggleSelector()
        snapHelper = PagerSnapHelper()
        snapHelper!!.attachToRecyclerView(rvPhotos)
        rvPhotos!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val view = snapHelper!!.findSnapView(lm) ?: return
                val position = lm!!.getPosition(view)
                if (lastPosition == position) {
                    return
                }
                lastPosition = position
                previewFragment!!.setSelectedPosition(-1)
                tvNumber!!.text = getString(
                    R.string.preview_current_number_easy_photos,
                    lastPosition + 1, photos.size
                )
                toggleSelector()
            }
        })
        tvNumber!!.text = getString(
            R.string.preview_current_number_easy_photos, index + 1,
            photos.size
        )
    }

    private var clickDone = false
    override fun onClick(v: View) {
        val id = v.id
        if (R.id.iv_back == id) {
            doBack()
        } else if (R.id.tv_selector == id) {
            updateSelector()
        } else if (R.id.iv_selector == id) {
            updateSelector()
        } else if (R.id.tv_done == id) {
            if (clickDone) return
            clickDone = true
            val intent = Intent()
            intent.putExtra(Key.PREVIEW_CLICK_DONE, true)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun toggleSelector() {
        if (photos[lastPosition].selected) {
            ivSelector!!.setImageResource(R.drawable.ic_selector_true_easy_photos)
            if (!Result.isEmpty()) {
                val count = Result.count()
                for (i in 0 until count) {
                    if (photos[lastPosition].path == Result.getPhotoPath(i)) {
                        previewFragment!!.setSelectedPosition(i)
                        break
                    }
                }
            }
        } else {
            ivSelector!!.setImageResource(R.drawable.ic_selector_easy_photos)
        }
        previewFragment!!.notifyDataSetChanged()
        shouldShowMenuDone()
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateSelector() {
        resultCode = RESULT_OK
        val item = photos[lastPosition]
        item.selectedOriginal = Setting.selectedOriginal
        if (isSingle) {
            singleSelector(item)
            return
        }
        if (unable) {
            if (item.selected) {
                Result.removePhoto(item)
                if (unable) {
                    unable = false
                }
                toggleSelector()
                return
            }
            if (Setting.isOnlyVideo()) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(
                            R.string.selector_reach_max_video_hint_easy_photos,
                            Setting.count
                        ),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
            } else if (Setting.showVideo) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(
                            R.string.max_select_notice,
                            Setting.count
                        ),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
            } else {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(
                            R.string.selector_reach_max_image_hint_easy_photos,
                            Setting.count
                        ),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
            }
            return
        }
        item.selected = !item.selected
        if (item.selected) {
            if (item.type.contains(Type.VIDEO) && item.size > Setting.videoMaxSize) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(R.string.selector_video_max_size),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
                item.selected = false
                return
            }
            if (!item.type.contains(Type.VIDEO) && item.size > Setting.photoMaxSize) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(R.string.selector_photo_max_size),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
                item.selected = false
                return
            }
            val res = Result.addPhoto(item)
            if (res != 0) {
                item.selected = false
                when (res) {
                    Result.PICTURE_OUT ->
                        MediaConfirmDialog(
                            MediaConfirmDialog.Config(
                                getString(
                                    R.string.selector_reach_max_image_hint_easy_photos,
                                    Setting.complexPictureCount
                                ),
                                getString(R.string.i_got_it)
                            )
                        ).show(
                            supportFragmentManager,
                            System.currentTimeMillis().toString()
                        )
                    Result.VIDEO_OUT ->
                        MediaConfirmDialog(
                            MediaConfirmDialog.Config(
                                getString(
                                    R.string.selector_reach_max_video_hint_easy_photos,
                                    Setting.complexVideoCount
                                ),
                                getString(R.string.i_got_it)
                            )
                        ).show(
                            supportFragmentManager,
                            System.currentTimeMillis().toString()
                        )
                    Result.SINGLE_TYPE ->
                        MediaConfirmDialog(
                            MediaConfirmDialog.Config(
                                getString(R.string.selector_single_type_hint_easy_photos),
                                getString(R.string.i_got_it)
                            )
                        ).show(
                            supportFragmentManager,
                            System.currentTimeMillis().toString()
                        )
                }
                return
            }
            if (Result.count() == Setting.count) {
                unable = true
            }
        } else {
            Result.removePhoto(item)
            previewFragment!!.setSelectedPosition(-1)
            if (unable) {
                unable = false
            }
        }
        toggleSelector()
        allSize
    }

    private val allSize: Unit
        get() {
            calculateFileSize()
        }

    private fun calculateFileSize() {
        if(Setting.selectedOriginal){
            var allSize: Long = 0
            for (i in Result.photos) {
                if (i.selected) {
                    allSize += i.size
                }
            }
            if(allSize!=0L){
                originalAllSize?.text =
                    String.format("共%s", FileUtils.getReadableFileSize(allSize.toInt()))
            }else{
                originalAllSize?.text =""
            }
        }
    }

    private fun singleSelector(photo: Photo) {
        if (!Result.isEmpty()) {
            if (Result.getPhotoPath(0) == photo.path) {
                Result.removePhoto(photo)
            } else {
                Result.removePhoto(0)
                Result.addPhoto(photo)
            }
        } else {
            Result.addPhoto(photo)
        }
        toggleSelector()
    }

    private fun shouldShowMenuDone() {
        if (Result.isEmpty()) {
            if (View.VISIBLE == tvDone!!.visibility) {
                val scaleHide = ScaleAnimation(1f, 0f, 1f, 0f)
                scaleHide.duration = 200
                tvDone!!.startAnimation(scaleHide)
            }
            tvDone!!.visibility = View.GONE
            flFragment!!.visibility = View.GONE
        } else {
            if (View.GONE == tvDone!!.visibility) {
                val scaleShow = ScaleAnimation(0f, 1f, 0f, 1f)
                scaleShow.duration = 200
                tvDone!!.startAnimation(scaleShow)
            }
            flFragment!!.visibility = View.VISIBLE
            tvDone!!.visibility = View.VISIBLE
            if (Result.isEmpty()) {
                return
            }
            if (Setting.complexSelector) {
                if (Setting.complexSingleType) {
                    if (Result.getPhotoType(0).contains(Type.VIDEO)) {
                        tvDone!!.text = getString(
                            R.string.selector_action_done_easy_photos, Result.count(),
                            Setting.complexVideoCount
                        )
                        return
                    }
                    tvDone!!.text = getString(
                        R.string.selector_action_done_easy_photos, Result.count(),
                        Setting.complexPictureCount
                    )
                    return
                }
            }
            tvDone!!.text = getString(
                R.string.selector_action_done_easy_photos, Result.count(),
                Setting.count
            )
        }
    }

    override fun onPreviewPhotoClick(position: Int) {
        val path = Result.getPhotoPath(position)
        val size = photos.size
        for (i in 0 until size) {
            if (TextUtils.equals(path, photos[i].path)) {
                rvPhotos!!.scrollToPosition(i)
                lastPosition = i
                tvNumber!!.text = getString(
                    R.string.preview_current_number_easy_photos,
                    lastPosition + 1, photos.size
                )
                previewFragment!!.setSelectedPosition(position)
                toggleSelector()
                return
            }
        }
    }

    private fun setClick(@IdRes vararg ids: Int) {
        for (id in ids) {
            findViewById<View>(id).setOnClickListener(this)
        }
    }

    private fun setClick(vararg views: View) {
        for (v in views) {
            v.setOnClickListener(this)
        }
    }

    companion object {
        fun start(act: Activity, albumItemIndex: Int, currIndex: Int) {
            val intent = Intent(act, PreviewActivity::class.java)
            intent.putExtra(Key.PREVIEW_ALBUM_ITEM_INDEX, albumItemIndex)
            intent.putExtra(Key.PREVIEW_PHOTO_INDEX, currIndex)
            act.startActivityForResult(intent, Code.REQUEST_PREVIEW_ACTIVITY)
        }

        /**
         * 一些旧设备在UI小部件更新之间需要一个小延迟
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}