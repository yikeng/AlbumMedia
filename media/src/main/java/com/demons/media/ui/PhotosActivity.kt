package com.demons.media.ui

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.demons.media.Gallery
import com.demons.media.R
import com.demons.media.constant.Code
import com.demons.media.constant.Key
import com.demons.media.constant.Type
import com.demons.media.models.ad.AdListener
import com.demons.media.models.album.AlbumModel
import com.demons.media.models.album.AlbumModel.CallBack
import com.demons.media.models.album.entity.Photo
import com.demons.media.result.Result
import com.demons.media.setting.Setting
import com.demons.media.ui.adapter.AlbumItemsAdapter
import com.demons.media.ui.adapter.PhotosAdapter
import com.demons.media.ui.dialog.LoadingDialog
import com.demons.media.ui.dialog.MediaConfirmDialog
import com.demons.media.ui.widget.PressedTextView
import com.demons.media.utils.Color.ColorUtils
import com.demons.media.utils.String.StringUtils
import com.demons.media.utils.bitmap.BitmapUtils
import com.demons.media.utils.file.FileUtils
import com.demons.media.utils.media.DurationUtils
import com.demons.media.utils.media.MediaScannerConnectionUtils
import com.demons.media.utils.permission.PermissionUtil
import com.demons.media.utils.permission.PermissionUtil.PermissionCallBack
import com.demons.media.utils.settings.SettingsUtils
import com.demons.media.utils.system.SystemUtils
import com.demons.media.utils.uri.UriUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ObjectAnimatorBinding")
class PhotosActivity : AppCompatActivity(), AlbumItemsAdapter.OnClickListener,
    PhotosAdapter.OnClickListener, AdListener, View.OnClickListener {
    private var mTempImageFile: File? = null
    private var albumModel: AlbumModel? = null
    private val photoList = ArrayList<Any?>()
    private val albumItemList = ArrayList<Any>()
    private val resultList = ArrayList<Photo>()
    private var rvPhotos: RecyclerView? = null
    private var photosAdapter: PhotosAdapter? = null
    private var gridLayoutManager: GridLayoutManager? = null
    private var rvAlbumItems: RecyclerView? = null
    private var albumItemsAdapter: AlbumItemsAdapter? = null
    private var rootViewAlbumItems: RelativeLayout? = null
    private var tvAlbumItems: PressedTextView? = null
    private var tvDone: PressedTextView? = null
    private var tvPreview: PressedTextView? = null
    private var tvOriginal: TextView? = null
    private var setHide: AnimatorSet? = null
    private var setShow: AnimatorSet? = null
    private var currAlbumItemIndex = 0
    private var ivCamera: ImageView? = null
    private var tvTitle: TextView? = null
    private var tvOriginalMenu: AppCompatCheckBox? = null
    private var originalAllSize: TextView? = null
    private var mSecondMenus: LinearLayout? = null
    private var permissionView: ConstraintLayout? = null
    private var tvPermission: TextView? = null
    private var mBottomBar: View? = null
    private var isQ = false
    private var loadingDialog: LoadingDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)
        hideActionBar()
        adaptationStatusBar()
        loadingDialog = LoadingDialog.get(this)
        isQ = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
        if (!Setting.onlyStartCamera && null == Setting.imageEngine) {
            finish()
            return
        }
        initSomeViews()
        if (PermissionUtil.checkAndRequestPermissionsInActivity(this, *needPermissions)) {
            hasPermissions()
        } else {
            permissionView!!.visibility = View.VISIBLE
        }
    }

    private fun adaptationStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var statusColor = window.statusBarColor
            if (statusColor == Color.TRANSPARENT) {
                statusColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
            }
            if (ColorUtils.isWhiteColor(statusColor)) {
                SystemUtils.getInstance().setStatusDark(this, true)
            }
        }
    }

    private fun initSomeViews() {
        mBottomBar = findViewById(R.id.m_bottom_bar)
        permissionView = findViewById(R.id.rl_permissions_view)
        tvPermission = findViewById(R.id.tv_permission)
        rootViewAlbumItems = findViewById(R.id.root_view_album_items)
        tvTitle = findViewById(R.id.tv_title)
        if (Setting.isOnlyVideo()) {
            tvTitle?.setText(R.string.video_selection_easy_photos)
        }
        findViewById<View>(R.id.iv_second_menu).visibility =
            if (Setting.showBottomMenu) View.VISIBLE else View.GONE
        tvOriginalMenu = findViewById(R.id.tv_original_checkbox)
        originalAllSize = findViewById<View>(R.id.original_all_size) as TextView
        tvOriginalMenu!!.setOnCheckedChangeListener { _, isChecked ->
            Setting.selectedOriginal = isChecked
            if (isChecked) {
                originalAllSize!!.visibility = View.VISIBLE
                calculateFileSize()
            } else {
                originalAllSize!!.visibility = View.INVISIBLE
            }
        }
        if (Setting.showOriginalMenu) {
            tvOriginalMenu?.visibility = View.VISIBLE
            tvOriginalMenu?.isChecked = Setting.selectedOriginal
        } else {
            tvOriginalMenu?.visibility = View.GONE
        }
        setClick(R.id.iv_back)
    }

    private fun hasPermissions() {
        permissionView!!.visibility = View.GONE
        if (Setting.onlyStartCamera) {
            launchCamera(Code.REQUEST_CAMERA)
            return
        }
        val albumModelCallBack = CallBack {
            runOnUiThread {
                loadingDialog!!.dismiss()
                onAlbumWorkedDo()
            }
        }
        loadingDialog!!.show()
        albumModel = AlbumModel.getInstance()
        albumModel?.query(this, albumModelCallBack)
    }

    protected val needPermissions: Array<String>
        get() = if (Setting.isShowCamera) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtil.onPermissionResult(this, permissions, grantResults,
            object : PermissionCallBack {
                override fun onSuccess() {
                    hasPermissions()
                }

                override fun onShouldShow() {
                    tvPermission!!.setText(R.string.permissions_again_easy_photos)
                    permissionView!!.setOnClickListener {
                        if (PermissionUtil.checkAndRequestPermissionsInActivity(
                                this@PhotosActivity,
                                *needPermissions
                            )
                        ) {
                            hasPermissions()
                        }
                    }
                }

                override fun onFailed() {
                    tvPermission!!.setText(R.string.permissions_die_easy_photos)
                    permissionView!!.setOnClickListener {
                        SettingsUtils.startMyApplicationDetailsForResult(
                            this@PhotosActivity,
                            packageName
                        )
                    }
                }
            })
    }

    /**
     * 启动相机
     *
     * @param requestCode startActivityForResult的请求码
     */
    private fun launchCamera(requestCode: Int) {
        if (TextUtils.isEmpty(Setting.fileProviderAuthority)) throw RuntimeException("AlbumBuilder" + " : 请执行 setFileProviderAuthority()方法")
        if (!cameraIsCanUse()) {
            permissionView!!.visibility = View.VISIBLE
            tvPermission!!.setText(R.string.permissions_die_easy_photos)
            permissionView!!.setOnClickListener {
                SettingsUtils.startMyApplicationDetailsForResult(
                    this@PhotosActivity,
                    packageName
                )
            }
            return
        }
        toAndroidCamera(requestCode)
    }

    /**
     * 启动系统相机
     *
     * @param requestCode 请求相机的请求码
     */
    private var photoUri: Uri? = null
    private fun toAndroidCamera(requestCode: Int) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null ||
            this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        ) {
            if (isQ) {
                photoUri = createImageUri()
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(cameraIntent, requestCode)
                return
            }
            createCameraTempImageFile()
            if (mTempImageFile != null && mTempImageFile!!.isFile) {
                val imageUri = UriUtils.getUri(this, mTempImageFile)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //对目标应用临时授权该Uri所代表的文件
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION) //对目标应用临时授权该Uri所代表的文件
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri) //将拍取的照片保存到指定URI
                startActivityForResult(cameraIntent, requestCode)
            } else {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(R.string.camera_temp_file_error_easy_photos),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
            }
        } else {
            MediaConfirmDialog(
                MediaConfirmDialog.Config(
                    getString(R.string.msg_no_camera_easy_photos),
                    getString(R.string.i_got_it)
                )
            ).show(
                supportFragmentManager,
                System.currentTimeMillis().toString()
            )
        }
    }

    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     */
    private fun createImageUri(): Uri? {
        //设置保存参数到ContentValues中
        val contentValues = ContentValues()
        //设置文件名
        contentValues.put(
            MediaStore.Images.Media.DISPLAY_NAME,
            System.currentTimeMillis().toString()
        )
        //兼容Android Q和以下版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
            //RELATIVE_PATH是相对路径不是绝对路径;照片存储的地方为：存储/Pictures
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
        }
        //设置文件类型
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG")
        //执行insert操作，向系统文件夹中添加文件
        //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
        return contentResolver.insert(
            MediaStore.Images.Media.getContentUri("external"),
            contentValues
        )
    }

    private fun createCameraTempImageFile() {
        var dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (null == dir) {
            dir = File(
                Environment.getExternalStorageDirectory(),
                File.separator + "DCIM" + File.separator + "Camera" + File.separator
            )
        }
        if (!dir.isDirectory) {
            if (!dir.mkdirs()) {
                dir = getExternalFilesDir(null)
                if (null == dir || !dir.exists()) {
                    dir = filesDir
                    if (null == dir || !dir.exists()) {
                        dir = filesDir
                        if (null == dir || !dir.exists()) {
                            val cacheDirPath =
                                File.separator + "data" + File.separator + "data" + File.separator + packageName + File.separator + "cache" + File.separator
                            dir = File(cacheDirPath)
                            if (!dir.exists()) {
                                dir.mkdirs()
                            }
                        }
                    }
                }
            }
        }
        mTempImageFile = try {
            File.createTempFile("IMG", ".jpg", dir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Code.REQUEST_SETTING_APP_DETAILS) {
            if (PermissionUtil.checkAndRequestPermissionsInActivity(this, *needPermissions)) {
                hasPermissions()
            } else {
                permissionView!!.visibility = View.VISIBLE
            }
            return
        }
        when (resultCode) {
            RESULT_OK -> {
                if (Code.REQUEST_CAMERA == requestCode) {
                    if (isQ) {
                        onCameraResultForQ()
                        return
                    }
                    if (mTempImageFile == null || !mTempImageFile!!.isFile) {
                        throw RuntimeException("AlbumMedia拍照保存的图片不存在")
                    }
                    onCameraResult()
                    return
                }
                if (Code.REQUEST_PREVIEW_ACTIVITY == requestCode) {
                    if (data!!.getBooleanExtra(Key.PREVIEW_CLICK_DONE, false)) {
                        done()
                        return
                    }
                    photosAdapter!!.change()
                    processOriginalMenu()
                    shouldShowMenuDone()
                    return
                }
                if (Code.REQUEST_PUZZLE_SELECTOR == requestCode) {
                    val puzzlePhoto = data!!.getParcelableExtra<Photo>(Gallery.RESULT_PHOTOS)
                    addNewPhoto(puzzlePhoto)
                    return
                }
            }
            RESULT_CANCELED -> {
                if (Code.REQUEST_CAMERA == requestCode) {
                    // 删除临时文件
                    if (mTempImageFile != null && mTempImageFile!!.exists()) {
                        mTempImageFile!!.delete()
                        mTempImageFile = null
                    }
                    if (Setting.onlyStartCamera) {
                        finish()
                    }
                    return
                }
                if (Code.REQUEST_PREVIEW_ACTIVITY == requestCode) {
                    processOriginalMenu()
                    return
                }
            }
            else -> {}
        }
    }

    private var folderPath: String? = null
    private var albumName: String? = null
    private fun addNewPhoto(photo: Photo?) {
        photo!!.selectedOriginal = Setting.selectedOriginal
        if (!isQ) {
            MediaScannerConnectionUtils.refresh(this, photo.path)
            folderPath = File(photo.path).parentFile.absolutePath
            albumName = StringUtils.getLastPathSegment(folderPath)
        }
        val albumItem_all_name = albumModel!!.getAllAlbumName(this)
        val albumItem = albumModel!!.album.getAlbumItem(albumItem_all_name)
        albumItem?.addImageItem(0, photo)
        albumModel!!.album.addAlbumItem(albumName, folderPath, photo.path, photo.uri)
        val albumItem1 = albumModel!!.album.getAlbumItem(albumName)
        if (albumItem1 != null) {
            albumModel!!.album.getAlbumItem(albumName).addImageItem(0, photo)
        }
        albumItemList.clear()
        albumItemList.addAll(albumModel!!.albumItems)
        if (Setting.hasAlbumItemsAd()) {
            var albumItemsAdIndex = 2
            if (albumItemList.size < albumItemsAdIndex + 1) {
                albumItemsAdIndex = albumItemList.size - 1
            }
            albumItemList.add(albumItemsAdIndex, Setting.albumItemsAdView)
        }
        albumItemsAdapter!!.notifyDataSetChanged()
        if (Setting.count == 1) {
            Result.clear()
            val res = Result.addPhoto(photo)
            onSelectorOutOfMax(res)
        } else {
            if (Result.count() >= Setting.count) {
                onSelectorOutOfMax(null)
            } else {
                val res = Result.addPhoto(photo)
                onSelectorOutOfMax(res)
            }
        }
        rvAlbumItems!!.scrollToPosition(0)
        albumItemsAdapter!!.setSelectedPosition(0)
        shouldShowMenuDone()
    }

    @SuppressLint("Range")
    private fun getPhoto(uri: Uri?): Photo? {
        var p: Photo? = null
        val path: String
        val name: String
        val dateTime: Long
        val type: String
        val size: Long
        var width = 0
        var height = 0
        var orientation = 0
        val projections = AlbumModel.getInstance().projections
        val shouldReadWidth = projections.size > 8
        val cursor = contentResolver.query(uri!!, projections, null, null, null) ?: return null
        val albumNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            path = cursor.getString(1)
            name = cursor.getString(2)
            dateTime = cursor.getLong(3)
            type = cursor.getString(4)
            size = cursor.getLong(5)
            if (shouldReadWidth) {
                width = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH))
                height = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT))
                orientation =
                    cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION))
                if (90 == orientation || 270 == orientation) {
                    val temp = width
                    width = height
                    height = temp
                }
            }
            if (albumNameCol > 0) {
                albumName = cursor.getString(albumNameCol)
                folderPath = albumName
            }
            p = Photo(name, uri, path, dateTime, width, height, orientation, size, 0, type)
        }
        cursor.close()
        return p
    }

    private fun onCameraResultForQ() {
        loadingDialog!!.show()
        Thread(Runnable {
            val photo = getPhoto(photoUri)
            if (photo == null) {
                Log.e("Gallery", "onCameraResultForQ() -》photo = null")
                return@Runnable
            }
            runOnUiThread(Runnable {
                loadingDialog!!.dismiss()
                if (Setting.onlyStartCamera || albumModel!!.albumItems.isEmpty()) {
                    val data = Intent()
                    photo.selectedOriginal = Setting.selectedOriginal
                    resultList.add(photo)
                    data.putParcelableArrayListExtra(Gallery.RESULT_PHOTOS, resultList)
                    data.putExtra(
                        Gallery.RESULT_SELECTED_ORIGINAL,
                        Setting.selectedOriginal
                    )
                    setResult(RESULT_OK, data)
                    finish()
                    return@Runnable
                }
                addNewPhoto(photo)
            })
        }).start()
    }

    private fun onCameraResult() {
        val loading = LoadingDialog.get(this)
        Thread(Runnable {
            val dateFormat = SimpleDateFormat(
                "yyyyMMdd_HH_mm_ss",
                Locale.getDefault()
            )
            val imageName = "IMG_%s.jpg"
            val filename = String.format(imageName, dateFormat.format(Date()))
            val reNameFile = File(mTempImageFile!!.parentFile, filename)
            if (!reNameFile.exists()) {
                if (mTempImageFile!!.renameTo(reNameFile)) {
                    mTempImageFile = reNameFile
                }
            }
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(mTempImageFile!!.absolutePath, options)
            MediaScannerConnectionUtils.refresh(this@PhotosActivity, mTempImageFile) //
            // 更新媒体库
            val uri = UriUtils.getUri(this@PhotosActivity, mTempImageFile)
            var width = 0
            var height = 0
            var orientation = 0
            if (Setting.useWidth) {
                width = options.outWidth
                height = options.outHeight
                var exif: ExifInterface? = null
                try {
                    exif = ExifInterface(mTempImageFile!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (null != exif) {
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        width = options.outHeight
                        height = options.outWidth
                    }
                }
            }
            val photo = Photo(
                mTempImageFile!!.name, uri,
                mTempImageFile!!.absolutePath,
                mTempImageFile!!.lastModified() / 1000, width, height, orientation,
                mTempImageFile!!.length(),
                DurationUtils.getDuration(mTempImageFile!!.absolutePath),
                options.outMimeType
            )
            runOnUiThread(Runnable {
                if (Setting.onlyStartCamera || albumModel!!.albumItems.isEmpty()) {
                    val data = Intent()
                    photo.selectedOriginal = Setting.selectedOriginal
                    resultList.add(photo)
                    data.putParcelableArrayListExtra(Gallery.RESULT_PHOTOS, resultList)
                    data.putExtra(
                        Gallery.RESULT_SELECTED_ORIGINAL,
                        Setting.selectedOriginal
                    )
                    setResult(RESULT_OK, data)
                    finish()
                    return@Runnable
                }
                addNewPhoto(photo)
            })
        }).start()
    }

    private fun onAlbumWorkedDo() {
        initView()
    }

    private fun initView() {
        if (albumModel!!.albumItems.isEmpty()) {
            if (Setting.isOnlyVideo()) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(R.string.no_videos_easy_photos),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
                finish()
                return
            }
            MediaConfirmDialog(
                MediaConfirmDialog.Config(
                    getString(R.string.no_photos_easy_photos),
                    getString(R.string.i_got_it)
                )
            ).show(
                supportFragmentManager,
                System.currentTimeMillis().toString()
            )
            if (Setting.isShowCamera) launchCamera(Code.REQUEST_CAMERA) else finish()
            return
        }
        Gallery.setAdListener(this)
        if (Setting.hasPhotosAd()) {
            findViewById<View>(R.id.m_tool_bar_bottom_line).visibility = View.GONE
        }
        ivCamera = findViewById(R.id.fab_camera)
        if (Setting.isShowCamera && Setting.isBottomRightCamera()) {
            ivCamera?.visibility = View.VISIBLE
        }
        if (!Setting.showPuzzleMenu) {
            findViewById<View>(R.id.tv_puzzle).visibility = View.GONE
        }
        mSecondMenus = findViewById(R.id.m_second_level_menu)
        val columns = resources.getInteger(R.integer.photos_columns_easy_photos)
        tvAlbumItems = findViewById(R.id.tv_album_items)
        tvAlbumItems?.text = albumModel!!.albumItems[0].name
        tvDone = findViewById(R.id.tv_done)
        rvPhotos = findViewById(R.id.rv_photos)
        (rvPhotos?.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations = false
        //去除item更新的闪光
        photoList.clear()
        photoList.addAll(albumModel!!.getCurrAlbumItemPhotos(0))
        var index = 0
        if (Setting.hasPhotosAd()) {
            photoList.add(index, Setting.photosAdView)
        }
        if (Setting.isShowCamera && !Setting.isBottomRightCamera()) {
            if (Setting.hasPhotosAd()) index = 1
            photoList.add(index, null)
        }
        photosAdapter = PhotosAdapter(this, photoList, this)
        gridLayoutManager = GridLayoutManager(this, columns)
        if (Setting.hasPhotosAd()) {
            gridLayoutManager!!.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == 0) {
                        gridLayoutManager!!.spanCount //独占一行
                    } else {
                        1 //只占一行中的一列
                    }
                }
            }
        }
        rvPhotos?.layoutManager = gridLayoutManager
        rvPhotos?.adapter = photosAdapter
        tvOriginal = findViewById(R.id.tv_original)
        if (Setting.showOriginalMenu) {
            processOriginalMenu()
        } else {
            tvOriginal?.visibility = View.GONE
        }
        tvPreview = findViewById(R.id.tv_preview)
        initAlbumItems()
        shouldShowMenuDone()
        setClick(
            R.id.iv_album_items,
            R.id.tv_clear,
            R.id.iv_second_menu,
            R.id.tv_puzzle
        )
        setClick(
            tvAlbumItems!!,
            rootViewAlbumItems!!,
            tvDone!!,
            tvOriginal!!,
            tvPreview!!,
            ivCamera!!
        )
    }

    private fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    private fun initAlbumItems() {
        rvAlbumItems = findViewById(R.id.rv_album_items)
        albumItemList.clear()
        albumItemList.addAll(albumModel!!.albumItems)
        if (Setting.hasAlbumItemsAd()) {
            var albumItemsAdIndex = 2
            if (albumItemList.size < albumItemsAdIndex + 1) {
                albumItemsAdIndex = albumItemList.size - 1
            }
            albumItemList.add(albumItemsAdIndex, Setting.albumItemsAdView)
        }
        albumItemsAdapter = AlbumItemsAdapter(this, albumItemList, 0, this)
        rvAlbumItems?.layoutManager = LinearLayoutManager(this)
        rvAlbumItems?.adapter = albumItemsAdapter
    }

    override fun onClick(v: View) {
        val id = v.id
        if (R.id.tv_album_items == id || R.id.iv_album_items == id) {
            showAlbumItems(View.GONE == rootViewAlbumItems!!.visibility)
        } else if (R.id.root_view_album_items == id) {
            showAlbumItems(false)
        } else if (R.id.iv_back == id) {
            onBackPressed()
        } else if (R.id.tv_done == id) {
            done()
        } else if (R.id.tv_clear == id) {
            if (Result.isEmpty()) {
                processSecondMenu()
                return
            }
            Result.removeAll()
            photosAdapter!!.change()
            shouldShowMenuDone()
            processSecondMenu()
        } else if (R.id.tv_original == id) {
            if (!Setting.originalMenuUsable) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        Setting.originalMenuUnusableHint,
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
                return
            }
            Setting.selectedOriginal = !Setting.selectedOriginal
            processOriginalMenu()
            processSecondMenu()
        } else if (R.id.tv_preview == id) {
            PreviewActivity.start(this@PhotosActivity, -1, 0)
        } else if (R.id.fab_camera == id) {
            launchCamera(Code.REQUEST_CAMERA)
        } else if (R.id.iv_second_menu == id) {
            processSecondMenu()
        } else if (R.id.tv_puzzle == id) {
            processSecondMenu()
            PuzzleSelectorActivity.start(this)
        }
    }

    fun processSecondMenu() {
        if (mSecondMenus == null) {
            return
        }
        if (View.VISIBLE == mSecondMenus!!.visibility) {
            mSecondMenus!!.visibility = View.INVISIBLE
            if (Setting.isShowCamera && Setting.isBottomRightCamera()) {
                ivCamera!!.visibility = View.VISIBLE
            }
        } else {
            mSecondMenus!!.visibility = View.VISIBLE
            if (Setting.isShowCamera && Setting.isBottomRightCamera()) {
                ivCamera!!.visibility = View.INVISIBLE
            }
        }
    }

    private var clickDone = false
    private fun done() {
        if (clickDone) return
        clickDone = true
        //        if (Setting.useWidth) {
//            resultUseWidth();
//            return;
//        }
        resultFast()
    }

    private fun resultUseWidth() {
        loadingDialog!!.show()
        Thread {
            val size = Result.photos.size
            try {
                for (i in 0 until size) {
                    val photo = Result.photos[i]
                    if (photo.width == 0 || photo.height == 0) {
                        BitmapUtils.calculateLocalImageSizeThroughBitmapOptions(photo)
                    }
                    if (BitmapUtils.needChangeWidthAndHeight(photo)) {
                        val h = photo.width
                        photo.width = photo.height
                        photo.height = h
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            runOnUiThread {
                loadingDialog!!.dismiss()
                resultFast()
            }
        }.start()
    }

    private fun resultFast() {
        val intent = Intent()
        Result.processOriginal()
        resultList.addAll(Result.photos)
        intent.putParcelableArrayListExtra(Gallery.RESULT_PHOTOS, resultList)
        intent.putExtra(
            Gallery.RESULT_SELECTED_ORIGINAL,
            Setting.selectedOriginal
        )
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun processOriginalMenu() {
        if (!Setting.showOriginalMenu) return
        if (Setting.selectedOriginal) {
            tvOriginal!!.setTextColor(ContextCompat.getColor(this, R.color.photos_fg_accent))
        } else {
            if (Setting.originalMenuUsable) {
                tvOriginal!!.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.photos_fg_primary
                    )
                )
            } else {
                tvOriginal!!.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.photos_fg_primary_dark
                    )
                )
            }
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
            rvAlbumItems, "translationY",
            mBottomBar!!.top.toFloat(), 0f
        )
        val alphaShow = ObjectAnimator.ofFloat(rootViewAlbumItems, "alpha", 0.0f, 1.0f)
        translationShow.duration = 300
        setShow = AnimatorSet()
        setShow!!.interpolator = AccelerateDecelerateInterpolator()
        setShow!!.play(translationShow).with(alphaShow)
    }

    private fun newHideAnim() {
        val translationHide = ObjectAnimator.ofFloat(
            rvAlbumItems, "translationY", 0f,
            mBottomBar!!.top.toFloat()
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
        this.currAlbumItemIndex = currAlbumItemIndex
        photoList.clear()
        photoList.addAll(albumModel!!.getCurrAlbumItemPhotos(currAlbumItemIndex))
        var index = 0
        if (Setting.hasPhotosAd()) {
            photoList.add(index, Setting.photosAdView)
        }
        if (Setting.isShowCamera && !Setting.isBottomRightCamera()) {
            if (Setting.hasPhotosAd()) index = 1
            photoList.add(index, null)
        }
        photosAdapter!!.change()
        rvPhotos!!.scrollToPosition(0)
    }

    private fun shouldShowMenuDone() {
        calculateFileSize()
        if (Result.isEmpty()) {
            if (View.VISIBLE == tvDone!!.visibility) {
                val scaleHide = ScaleAnimation(1f, 0f, 1f, 0f)
                scaleHide.duration = 200
                tvDone!!.startAnimation(scaleHide)
            }
            tvDone!!.visibility = View.INVISIBLE
            tvPreview!!.visibility = View.INVISIBLE
        } else {
            if (View.INVISIBLE == tvDone!!.visibility) {
                val scaleShow = ScaleAnimation(0f, 1f, 0f, 1f)
                scaleShow.duration = 200
                tvDone!!.startAnimation(scaleShow)
            }
            tvDone!!.visibility = View.VISIBLE
            tvPreview!!.visibility = View.VISIBLE
        }
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

    override fun onCameraClick() {
        launchCamera(Code.REQUEST_CAMERA)
    }

    override fun onPhotoClick(position: Int, realPosition: Int) {
        PreviewActivity.start(this@PhotosActivity, currAlbumItemIndex, realPosition)
    }

    override fun onResume() {
        super.onResume()
        tvOriginalMenu?.isChecked = Setting.selectedOriginal
        calculateFileSize()
    }

    override fun onSelectorOutOfMax(result: Int?) {
        if (result == null) {
            if (Setting.isOnlyVideo()) {
                MediaConfirmDialog(
                    MediaConfirmDialog.Config(
                        getString(
                            R.string.selector_reach_max_video_hint_easy_photos, Setting.count
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
        when (result) {
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
                        getString(
                            R.string.selector_single_type_hint_easy_photos
                        ),
                        getString(R.string.i_got_it)
                    )
                ).show(
                    supportFragmentManager,
                    System.currentTimeMillis().toString()
                )
        }
    }

    override fun onSelectorChanged() {
        shouldShowMenuDone()
    }

    override fun onBackPressed() {
        if (null != rootViewAlbumItems && rootViewAlbumItems!!.visibility == View.VISIBLE) {
            showAlbumItems(false)
            return
        }
        if (null != mSecondMenus && View.VISIBLE == mSecondMenus!!.visibility) {
            processSecondMenu()
            return
        }
        if (albumModel != null) albumModel!!.stopQuery()
        if (Setting.hasPhotosAd()) {
            photosAdapter!!.clearAd()
        }
        if (Setting.hasAlbumItemsAd()) {
            albumItemsAdapter!!.clearAd()
        }
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        if (albumModel != null) albumModel!!.stopQuery()
        super.onDestroy()
    }

    override fun onPhotosAdLoaded() {
        runOnUiThread { photosAdapter!!.change() }
    }

    override fun onAlbumItemsAdLoaded() {
        runOnUiThread { albumItemsAdapter!!.notifyDataSetChanged() }
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

    /**
     * 返回true 表示可以使用  返回false表示不可以使用
     */
    fun cameraIsCanUse(): Boolean {
        var isCanUse = true
        var mCamera: Camera? = null
        try {
            mCamera = Camera.open()
            val mParameters = mCamera.parameters //针对魅族手机
            mCamera.parameters = mParameters
        } catch (e: Exception) {
            isCanUse = false
        }
        if (mCamera != null) {
            try {
                mCamera.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return isCanUse
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

    companion object {
        var startTime: Long = 0
        fun doubleClick(): Boolean {
            val now = System.currentTimeMillis()
            if (now - startTime < 600) {
                return true
            }
            startTime = now
            return false
        }

        @JvmStatic
        fun start(activity: Activity, requestCode: Int) {
            if (doubleClick()) return
            val intent = Intent(activity, PhotosActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }

        @JvmStatic
        fun start(fragment: Fragment, requestCode: Int) {
            if (doubleClick()) return
            val intent = Intent(fragment.activity, PhotosActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }

        @JvmStatic
        fun start(fragment: androidx.fragment.app.Fragment, requestCode: Int) {
            if (doubleClick()) return
            val intent = Intent(fragment.context, PhotosActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}