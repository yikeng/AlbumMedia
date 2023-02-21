package com.demons.album

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.demons.album.codec.*
import com.demons.media.Gallery
import com.demons.media.callback.PuzzleCallback
import com.demons.media.callback.SelectCallback
import com.demons.media.constant.Type
import com.demons.media.models.album.entity.Photo
import com.demons.media.setting.Setting
import com.demons.album.codec.MediaCodecUtils.updateSourceMedia
import com.demons.album.codec.MediaCodecUtils.updateTrimConfig
import com.demons.media.utils.permission.PermissionUtil
import com.google.android.material.navigation.NavigationView
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.io.MediaRange
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.TimeUnit


open class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    /**
     * 选择的图片集
     */
    private val selectedPhotoList = ArrayList<Photo?>()
    private var adapter: MainAdapter? = null
    private var rvImage: RecyclerView? = null

    /**
     * 图片列表和专辑项目列表的广告view
     */
    private var photosAdView: RelativeLayout? = null
    private var albumItemsAdView: RelativeLayout? = null

    /**
     * 广告是否加载完成
     */
    private var photosAdLoaded = false
    private val albumItemsAdLoaded = false

    /**
     * 展示bitmap功能的
     */
    private var bitmap: Bitmap? = null
    private var bitmapView: ImageView? = null
    private var drawer: DrawerLayout? = null
    private var mediaTransformer: MediaTransformer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        initView()
        if (PermissionUtil.checkAndRequestPermissionsInActivity(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            preLoadAlbums()
        }
        mediaTransformer = MediaTransformer(applicationContext)
    }

    /**
     * 预加载相册扫描，可以增加点速度，写不写都行
     * 该方法如果没有授权读取权限的话，是无效的，所以外部加不加权限控制都可以，加的话保证执行，不加也不影响程序正常使用。
     */
    private fun preLoadAlbums() {
        Gallery.preLoad(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtil.onPermissionResult(this, permissions, grantResults,
            object : PermissionUtil.PermissionCallBack {
                override fun onSuccess() {
                    preLoadAlbums()
                }

                override fun onShouldShow() {}
                override fun onFailed() {}
            })
    }

    private fun initView() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        //        setSupportActionBar(toolbar);
        drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer!!.openDrawer(GravityCompat.START)
        drawer!!.clearAnimation()
        drawer!!.animation = null
        drawer!!.layoutAnimation = null
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer!!.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.clearAnimation()
        navigationView.animation = null
        navigationView.layoutAnimation = null
        bitmapView = findViewById(R.id.iv_image)
        bitmapView?.setOnClickListener { bitmapView?.visibility = View.GONE }
        rvImage = findViewById<View>(R.id.rv_image) as RecyclerView
        val linearLayoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL, false
        )
        adapter = MainAdapter(this, selectedPhotoList)
        rvImage!!.layoutManager = linearLayoutManager
        rvImage!!.adapter = adapter
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rvImage)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sample, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            val intent = Intent(this, GalleryShowActivity::class.java)
            startActivity(intent)
        }
        return id == R.id.action_settings || super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        bitmapView!!.visibility = View.GONE
        when (item.itemId) {
            R.id.test -> Gallery.createAlbum(this, false, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(30)
                .setVideoMaxSize(1048576 * 1024)
                .setVideo(true)
                .setUseWidth(true)
                .setOriginalMenu(false, true, null)
                .setGif(true)
                .showBottomMenu(false)
                .start(101)
            R.id.camera -> Gallery.createCamera(this, true)
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .start(101) //也可以选择链式调用写法
            R.id.album_single -> Gallery.createAlbum(this, false, false, GlideEngine())
                .start(101) //也可以选择链式调用写法
            R.id.album_multi -> Gallery.createAlbum(this, false, false, GlideEngine())
                .setCount(9)
                .start(101) //也可以选择链式调用写法
            R.id.album_camera_single -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .start(101) //也可以选择链式调用写法
            R.id.album_camera_multi -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(22)
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>, isOriginal: Boolean) {
                        selectedPhotoList.clear()
                        selectedPhotoList.addAll(photos)
                        adapter!!.notifyDataSetChanged()
                        rvImage!!.smoothScrollToPosition(0)
                    }

                    override fun onCancel() {
//                                Toast.makeText(SampleActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                    }
                })
            R.id.album_camera_multi_use_width -> Gallery.createAlbum(
                this,
                true,
                true,
                GlideEngine()
            )
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(22)
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>, isOriginal: Boolean) {
                        selectedPhotoList.clear()
                        selectedPhotoList.addAll(photos)
                        adapter!!.notifyDataSetChanged()
                        rvImage!!.smoothScrollToPosition(0)
                    }

                    override fun onCancel() {
//                                Toast.makeText(SampleActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                    }
                })
            R.id.album_complex_selector1 -> Gallery.createAlbum(this, false, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .complexSelector(false, 3, 9) //参数说明：是否只能选择单类型，视频数，图片数。
                .setFileCount(9)
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>, isOriginal: Boolean) {
                        selectedPhotoList.clear()
                        selectedPhotoList.addAll(photos)
                        adapter!!.notifyDataSetChanged()
                        rvImage!!.smoothScrollToPosition(0)
                    }

                    override fun onCancel() {
//                                Toast.makeText(SampleActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                    }
                })
            R.id.album_complex_selector2 -> Gallery.createAlbum(this, false, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .complexSelector(true, 2, 3) //参数说明：是否只能选择单类型，视频数，图片数。
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>, isOriginal: Boolean) {
                        selectedPhotoList.clear()
                        selectedPhotoList.addAll(photos)
                        adapter!!.notifyDataSetChanged()
                        rvImage!!.smoothScrollToPosition(0)
                    }

                    override fun onCancel() {
//                                Toast.makeText(SampleActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                    }
                })
            R.id.album_ad ->
                // 需要在启动前创建广告view
                // 广告view不能有父布局
                // 广告view可以包含子布局
                // 广告View的数据可以在任何时候绑定
//                initAdViews();

                //启动方法，装载广告view
                Gallery.createAlbum(this, true, false, GlideEngine())
                    .setFileProviderAuthority("com.demo.gallery.fileprovider")
                    .setCount(9)
                    .setCameraLocation(Setting.LIST_FIRST)
                    .setAdView(
                        photosAdView, photosAdLoaded, albumItemsAdView,
                        albumItemsAdLoaded
                    )
                    .setVideo(true)
                    .setOriginalMenu(false, true, null)
                    .setGif(true)
                    .showBottomMenu(false)
                    .start(101)
            R.id.album_size -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(9)
                .setMinWidth(500)
                .setMinHeight(500)
                .setMinFileSize((1024 * 10).toLong())
                .start(101)
            R.id.album_original_usable -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(9)
                .setOriginalMenu(true, true, null)
                .start(101)
            R.id.album_original_unusable -> {
                val isVip = false //假设获取用户信息发现该用户不是vip
                Gallery.createAlbum(this, true, false, GlideEngine())
                    .setFileProviderAuthority("com.demo.gallery.fileprovider")
                    .setCount(9)
                    .setOriginalMenu(false, isVip, "该功能为VIP会员特权功能")
                    .start(101)
            }
            R.id.album_has_video_gif -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(9)
                .setVideo(true)
                .setGif(true)
                .start(101)
            R.id.album_only_video -> Gallery.createAlbum(this, true, true, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(9)
                .filter(Type.VIDEO)
                .start(101)
            R.id.album_no_menu -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setCount(9)
                .setPuzzleMenu(false)
                .setCleanMenu(false)
                .start(101)
            R.id.album_selected -> Gallery.createAlbum(this, true, false, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setPuzzleMenu(false)
                .setCount(9)
                .setSelectedPhotos(selectedPhotoList) //当传入已选中图片时，按照之前选中的顺序排序
                //                        .setSelectedPhotos(selectedPhotoList,false)//当传入已选中图片时，不按照之前选中的顺序排序
                //                        .setSelectedPhotoPaths(selectedPhotoPathList)//两种方式参数类型不同，根据情况任选
                .start(101)
            R.id.addWatermark -> Gallery.createAlbum(this, false, true, GlideEngine())
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .setPuzzleMenu(false)
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>, isOriginal: Boolean) {
                        selectedPhotoList.clear()
                        adapter!!.notifyDataSetChanged()

                        //这一步如果图大的话会耗时，但耗时不长，建议在异步操作。另外copy出来的bitmap在确定不用的时候记得回收，如果你用Glide操作过copy
                        // 出来的bitmap那就不要回收了，否则Glide会报错。
                        val watermark = BitmapFactory.decodeResource(
                            resources,
                            R.mipmap.watermark
                        ).copy(Bitmap.Config.RGB_565, true)
                        try {
                            bitmap =
                                BitmapFactory.decodeStream(contentResolver.openInputStream(photos[0].uri))
                                    .copy(Bitmap.Config.ARGB_8888, true)
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                        //给图片添加水印的api
                        bitmap = Gallery.addWatermark(
                            watermark,
                            bitmap,
                            1080,
                            20,
                            20,
                            true,
                            photos[0].orientation
                        )
                        bitmapView!!.visibility = View.VISIBLE
                        bitmapView!!.setImageBitmap(bitmap)
                        Toast.makeText(this@MainActivity, "水印在左下角", Toast.LENGTH_SHORT).show()
                    }

                    override fun onCancel() {}
                })
            R.id.puzzle -> Gallery.createAlbum(this, false, false, GlideEngine())
                .setCount(9)
                .setPuzzleMenu(false)
                .setFileProviderAuthority("com.demo.gallery.fileprovider")
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>, isOriginal: Boolean) {
                        Gallery.startPuzzleWithPhotos(this@MainActivity,
                            photos,
                            Environment.getExternalStorageDirectory().absolutePath,
                            "AlbumBuilder",
                            false,
                            GlideEngine(),
                            object : PuzzleCallback() {
                                override fun onResult(photo: Photo) {
                                    selectedPhotoList.clear()
                                    selectedPhotoList.add(photo)
                                    adapter!!.notifyDataSetChanged()
                                    rvImage!!.smoothScrollToPosition(0)
                                }

                                override fun onCancel() {
                                    Toast.makeText(
                                        this@MainActivity, "Cancel",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }

                    override fun onCancel() {
                        Toast.makeText(this@MainActivity, "Cancel", Toast.LENGTH_SHORT).show()
                    }
                })
            R.id.face_detection -> {}
            else -> {}
        }
        return true
    }

    /**
     * 需要在启动前创建广告view
     * 广告view不能有父布局
     * 广告view可以包含子布局
     * 为了确保广告view地址不变，设置final会更安全
     */
    private fun initAdViews() {

        //模拟启动EasyPhotos前广告已经装载完毕
        initPhotosAd()

        //模拟不确定启动EasyPhotos前广告是否装载完毕
        initAlbumItemsAd()
    }

    /**
     * 模拟启动EasyPhotos前广告已经装载完毕
     */
    private fun initPhotosAd() {
        photosAdView = layoutInflater.inflate(
            R.layout.ad_photos, null,
            false
        ) as RelativeLayout //不可以有父布局，所以inflate第二个参数必须为null，并且布局文件必须独立
        (photosAdView!!.findViewById<View>(R.id.tv_title) as TextView).text = "photosAd广告"
        (photosAdView!!.findViewById<View>(R.id.tv_content) as TextView).text = "github上star" +
                "一下了解EasyPhotos的最新动态,这个布局和数据都是由你定制的"
        photosAdLoaded = true
    }

    /**
     * 模拟不确定启动EasyPhotos前广告是否装载完毕
     * 模拟5秒后网络回调
     */
    private fun initAlbumItemsAd() {
        albumItemsAdView = layoutInflater.inflate(
            R.layout.ad_album_items,
            null, false
        ) as RelativeLayout //不可以有父布局，所以inflate第二个参数必须为null，并且布局文件必须独立

        //模拟5秒后网络回调
        rvImage!!.postDelayed({
            (albumItemsAdView!!.findViewById<View>(R.id.iv_image) as ImageView).setImageResource(R.mipmap.ad)
            (albumItemsAdView!!.findViewById<View>(R.id.tv_title) as TextView).text =
                "albumItemsAd广告"
            photosAdLoaded = true //正常情况可能不知道是先启动EasyPhotos还是数据先回来，所以这里加个标识，如果是后启动EasyPhotos
            // ，那么EasyPhotos会直接加载广告
            Gallery.notifyAlbumItemsAdLoaded()
        }, 5000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (albumItemsAdView != null) {
            if (albumItemsAdView!!.parent != null) {
                (albumItemsAdView!!.parent as FrameLayout).removeAllViews()
            }
        }
        if (photosAdView != null) {
            if (photosAdView!!.parent != null) {
                (photosAdView!!.parent as FrameLayout).removeAllViews()
            }
        }
        if (RESULT_OK == resultCode) {
            //相机或相册回调
            if (requestCode == 101) {
                //返回对象集合：如果你需要了解图片的宽、高、大小、用户是否选中原图选项等信息，可以用这个
                val resultPhotos = data!!.getParcelableArrayListExtra<Photo?>(Gallery.RESULT_PHOTOS)
                //返回图片地址集合时如果你需要知道用户选择图片时是否选择了原图选项，用如下方法获取
                val selectedOriginal = data.getBooleanExtra(Gallery.RESULT_SELECTED_ORIGINAL, false)
                selectedPhotoList.clear()
                selectedPhotoList.addAll(resultPhotos!!)
                adapter!!.notifyDataSetChanged()
                rvImage!!.smoothScrollToPosition(0)
                // TODO: 如果非原视频就做转码压缩处理
                for (i in resultPhotos) {
                    if (i.type.contains(Type.VIDEO)) {
                        val sourceMedia = SourceMedia()
                        val transformationState =
                            TransformationState()
                        val targetMedia = TargetMedia()
                        val trimConfig = TrimConfig()
                        updateSourceMedia(this, sourceMedia, i.uri)
                        updateTrimConfig(trimConfig, sourceMedia)
                        val targetFile = File(
                            TransformationUtil.getTargetFileDirectory(applicationContext),
                            "transcoded_" + TransformationUtil.getDisplayName(this, sourceMedia.uri)
                        )
                        targetMedia.setTargetFile(targetFile)
                        targetMedia.setTracks(sourceMedia.tracks)
                        transformationState.setState(TransformationState.STATE_IDLE)
                        transformationState.setStats(null)
                        transcode(sourceMedia, targetMedia, trimConfig, transformationState)
                    }
                }
                return
            }

            //为拼图选择照片的回调
            if (requestCode == 102) {
                val resultPhotos = data!!.getParcelableArrayListExtra<Photo?>(Gallery.RESULT_PHOTOS)
                if (resultPhotos!!.size == 1) {
                    resultPhotos.add(resultPhotos[0])
                }
                selectedPhotoList.clear()
                selectedPhotoList.addAll(resultPhotos)
                Gallery.startPuzzleWithPhotos(
                    this, selectedPhotoList,
                    TransformationUtil.getTargetFileDirectory(applicationContext).absolutePath,
                    "AlbumBuilder", 103, false, GlideEngine()
                )
                return
            }

            //拼图回调
            if (requestCode == 103) {
                val puzzlePhoto = data!!.getParcelableExtra<Photo>(Gallery.RESULT_PHOTOS)
                selectedPhotoList.clear()
                selectedPhotoList.add(puzzlePhoto)
                adapter!!.notifyDataSetChanged()
                rvImage!!.smoothScrollToPosition(0)
            }
        } else if (RESULT_CANCELED == resultCode) {
            Toast.makeText(applicationContext, "cancel", Toast.LENGTH_SHORT).show()
        }
    }

    fun transcode(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        trimConfig: TrimConfig,
        transformationState: TransformationState
    ) {
        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete()
        }

        transformationState.requestId = UUID.randomUUID().toString()
        val transformationListener =
            MediaTransformationListener(
                this,
                transformationState.requestId,
                transformationState,
                targetMedia
            )

        val mediaRange = if (trimConfig.enabled) MediaRange(
            TimeUnit.MILLISECONDS.toMicros((trimConfig.range[0] * 1000).toLong()),
            TimeUnit.MILLISECONDS.toMicros((trimConfig.range[1] * 1000).toLong())
        ) else MediaRange(0, Long.MAX_VALUE)

        val transformationOptions = TransformationOptions.Builder()
            .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
            .setSourceMediaRange(mediaRange)
            .setRemoveMetadata(true)
            .build()

        val targetVideoFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            1280,
            720
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
        }

        mediaTransformer?.transform(
            transformationState.requestId,
            sourceMedia.uri,
            targetMedia.targetFile.path,
            targetVideoFormat,
            null,
            transformationListener,
            transformationOptions
        )
    }

    override fun onBackPressed() {
        if (drawer!!.isDrawerOpen(GravityCompat.START)) {
            drawer!!.closeDrawer(GravityCompat.START)
            return
        }
        super.onBackPressed()
    }
}