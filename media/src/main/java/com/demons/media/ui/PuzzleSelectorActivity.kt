package com.demons.media.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.demons.media.R
import com.demons.media.constant.Code
import com.demons.media.models.album.AlbumModel
import com.demons.media.models.album.entity.Photo
import com.demons.media.setting.Setting
import com.demons.media.ui.adapter.AlbumItemsAdapter
import com.demons.media.ui.adapter.PuzzleSelectorAdapter
import com.demons.media.ui.adapter.PuzzleSelectorPreviewAdapter
import com.demons.media.ui.dialog.MediaConfirmDialog
import com.demons.media.ui.widget.PressedTextView
import com.demons.media.utils.Color.ColorUtils
import com.demons.media.utils.system.SystemUtils
@SuppressLint("ObjectAnimatorBinding")
class PuzzleSelectorActivity : AppCompatActivity(), View.OnClickListener,
    AlbumItemsAdapter.OnClickListener, PuzzleSelectorAdapter.OnClickListener,
    PuzzleSelectorPreviewAdapter.OnClickListener {
    private var albumModel: AlbumModel? = null
    private var setShow: AnimatorSet? = null
    private var setHide: AnimatorSet? = null
    private var rootViewAlbumItems: RelativeLayout? = null
    private var rootSelectorView: ConstraintLayout? = null
    private var rvAlbumItems: RecyclerView? = null
    private var albumItemsAdapter: AlbumItemsAdapter? = null
    private var tvAlbumItems: PressedTextView? = null
    private val photoList = ArrayList<Photo>()
    private var photosAdapter: PuzzleSelectorAdapter? = null
    private var rvPhotos: RecyclerView? = null
    private var rvPreview: RecyclerView? = null
    private var previewAdapter: PuzzleSelectorPreviewAdapter? = null
    private val selectedPhotos = ArrayList<Photo>()
    private var tvDone: PressedTextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle_selector)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var statusColor = window.statusBarColor
            if (statusColor == Color.TRANSPARENT) {
                statusColor = ContextCompat.getColor(this, R.color.photos_status_bar)
            }
            if (ColorUtils.isWhiteColor(statusColor)) {
                SystemUtils.getInstance().setStatusDark(this, true)
            }
        }
        albumModel = AlbumModel.getInstance()
        //        albumModel.query(this, null);
        if (null == albumModel || albumModel!!.albumItems.isEmpty()) {
            finish()
            return
        }
        initView()
    }

    private fun initView() {
        setClick(R.id.iv_back)
        tvAlbumItems = findViewById<View>(R.id.tv_album_items) as PressedTextView
        tvAlbumItems!!.text = albumModel!!.albumItems[0].name
        rootSelectorView = findViewById(R.id.m_selector_root)
        tvDone = findViewById<View>(R.id.tv_done) as PressedTextView
        tvDone!!.setOnClickListener(this)
        tvAlbumItems!!.setOnClickListener(this)
        initAlbumItems()
        initPhotos()
        initPreview()
    }

    private fun initPreview() {
        rvPreview = findViewById<View>(R.id.rv_preview_selected_photos) as RecyclerView
        val lm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        previewAdapter = PuzzleSelectorPreviewAdapter(this, selectedPhotos, this)
        rvPreview!!.layoutManager = lm
        rvPreview!!.adapter = previewAdapter
    }

    private fun initPhotos() {
        rvPhotos = findViewById<View>(R.id.rv_photos) as RecyclerView
        (rvPhotos!!.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations =
            false //去除item更新的闪光
        photoList.addAll(albumModel!!.getCurrAlbumItemPhotos(0))
        photosAdapter = PuzzleSelectorAdapter(this, photoList, this)
        val columns = resources.getInteger(R.integer.photos_columns_easy_photos)
        val gridLayoutManager = GridLayoutManager(this, columns)
        rvPhotos!!.layoutManager = gridLayoutManager
        rvPhotos!!.adapter = photosAdapter
    }

    private fun initAlbumItems() {
        rootViewAlbumItems = findViewById<View>(R.id.root_view_album_items) as RelativeLayout
        rootViewAlbumItems!!.setOnClickListener(this)
        setClick(R.id.iv_album_items)
        rvAlbumItems = findViewById<View>(R.id.rv_album_items) as RecyclerView
        val lm = LinearLayoutManager(this)
        val list = ArrayList<Any>(albumModel!!.albumItems)
        albumItemsAdapter = AlbumItemsAdapter(this, list, 0, this)
        rvAlbumItems!!.layoutManager = lm
        rvAlbumItems!!.adapter = albumItemsAdapter
    }

    private fun setClick(@IdRes vararg ids: Int) {
        for (id in ids) {
            findViewById<View>(id).setOnClickListener(this)
        }
    }

    override fun onClick(view: View) {
        val id = view.id
        if (R.id.iv_back == id) {
            setResult(RESULT_CANCELED)
            finish()
        } else if (R.id.tv_album_items == id || R.id.iv_album_items == id) {
            showAlbumItems(View.GONE == rootViewAlbumItems!!.visibility)
        } else if (R.id.root_view_album_items == id) {
            showAlbumItems(false)
        } else if (R.id.tv_done == id) {
            PuzzleActivity.startWithPhotos(
                this,
                selectedPhotos,
                Environment.getExternalStorageDirectory().absolutePath + "/" + getString(R.string.app_name),
                "IMG",
                Code.REQUEST_PUZZLE,
                false,
                Setting.imageEngine
            )
        }
    }

    private fun showAlbumItems(isShow: Boolean) {
        if (null == setShow) {
            newAnimators()
        }
        if (isShow) {
            rootViewAlbumItems!!.visibility = View.VISIBLE
            setShow!!.start()
        } else {
            setHide!!.start()
        }
    }

    private fun newAnimators() {
        newHideAnim()
        newShowAnim()
    }

    private fun newShowAnim() {
        val translationShow = ObjectAnimator.ofFloat(
            rvAlbumItems,
            "translationY",
            rootSelectorView!!.top.toFloat(),
            0f
        )
        val alphaShow = ObjectAnimator.ofFloat(rootViewAlbumItems, "alpha", 0.0f, 1.0f)
        translationShow.duration = 300
        setShow = AnimatorSet()
        setShow!!.interpolator = AccelerateDecelerateInterpolator()
        setShow!!.play(translationShow).with(alphaShow)
    }

    private fun newHideAnim() {
        val translationHide = ObjectAnimator.ofFloat(
            rvAlbumItems,
            "translationY",
            0f,
            rootSelectorView!!.top.toFloat()
        )
        val alphaHide = ObjectAnimator.ofFloat(rootViewAlbumItems, "alpha", 1.0f, 0.0f)
        translationHide.duration = 200
        setHide = AnimatorSet()
        setHide!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                rootViewAlbumItems!!.visibility = View.GONE
            }
        })
        setHide!!.interpolator = AccelerateInterpolator()
        setHide!!.play(translationHide).with(alphaHide)
    }

    override fun onAlbumItemClick(position: Int, realPosition: Int) {
        updatePhotos(realPosition)
        showAlbumItems(false)
        tvAlbumItems!!.text = albumModel!!.albumItems[realPosition].name
    }

    private fun updatePhotos(currAlbumItemIndex: Int) {
        photoList.clear()
        photoList.addAll(albumModel!!.getCurrAlbumItemPhotos(currAlbumItemIndex))
        photosAdapter?.itemCount?.let { photosAdapter?.notifyItemRangeChanged(0, it) }
        rvPhotos!!.scrollToPosition(0)
    }

    override fun onBackPressed() {
        if (null != rootViewAlbumItems && rootViewAlbumItems!!.visibility == View.VISIBLE) {
            showAlbumItems(false)
            return
        }
        super.onBackPressed()
    }

    override fun onPhotoClick(position: Int) {
        if (selectedPhotos.size > Setting.count-1) {
            MediaConfirmDialog(
                MediaConfirmDialog.Config(
                    getString(R.string.selector_reach_max_image_hint_easy_photos, Setting.count),
                    getString(R.string.i_got_it)
                )
            ).show(
                supportFragmentManager,
                System.currentTimeMillis().toString()
            )
            return
        }
        selectedPhotos.add(photoList[position])
        previewAdapter?.itemCount?.let { previewAdapter?.notifyItemRangeChanged(0, it) }
        rvPreview!!.smoothScrollToPosition(selectedPhotos.size - 1)
        tvDone!!.text = getString(R.string.selector_action_done_easy_photos, selectedPhotos.size, Setting.count)
        if (selectedPhotos.size > 1) {
            tvDone!!.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        selectedPhotos.removeAt(position)
        previewAdapter?.itemCount?.let { previewAdapter?.notifyItemRangeChanged(0, it) }
        tvDone!!.text = getString(R.string.selector_action_done_easy_photos, selectedPhotos.size, Setting.count)
        if (selectedPhotos.size < 2) {
            tvDone!!.visibility = View.INVISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == Code.REQUEST_PUZZLE) {
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, PuzzleSelectorActivity::class.java)
            activity.startActivityForResult(intent, Code.REQUEST_PUZZLE_SELECTOR)
        }
    }
}