package com.nooras.watermark

import android.app.Activity
import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.*
import java.io.*
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var logoView: ImageView
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var size: SeekBar
    private lateinit var opacity: SeekBar
    private lateinit var xpos: SeekBar
    private lateinit var ypos: SeekBar
    private lateinit var rotation: SeekBar
    private lateinit var discOn: CheckBox
    private lateinit var discText: EditText
    private lateinit var discSize: SeekBar
    private lateinit var discOpacity: SeekBar
    private lateinit var format: Spinner
    private var logo: Bitmap? = null
    private val photos=mutableListOf<Uri>()
    private val pool=Executors.newSingleThreadExecutor()

    private fun dp(n:Int)= (n*resources.displayMetrics.density).toInt()
    private fun tv(s:String,sp:Float=15f)=TextView(this).apply{text=s;textSize=sp;setPadding(dp(4),dp(7),dp(4),dp(4))}
    private fun slider(title:String,min:Int,max:Int,initial:Int):SeekBar{
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val t=tv("$title: $initial",14f); val b=SeekBar(this).apply{this.max=max-min;progress=initial-min}
        b.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){t.text="$title: ${p+min}";if(u)showPreview()}
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){}
        })
        box.addView(t);box.addView(b);(root!!).addView(box);return b
    }
    private var root:LinearLayout?=null

    override fun onCreate(b:Bundle?){super.onCreate(b);build();loadDefaultLogo()}
    private fun build(){
        val scroll=ScrollView(this)
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(24))}
        scroll.addView(root);setContentView(scroll)
        root!!.addView(tv("NOORAS WATERMARK",25f).apply{gravity=Gravity.CENTER})
        root!!.addView(tv("Offline photo protection tool",13f).apply{gravity=Gravity.CENTER})
        root!!.addView(tv("LOGO",19f))
        logoView=ImageView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,dp(160));scaleType=ImageView.ScaleType.CENTER_INSIDE}
        root!!.addView(logoView)
        root!!.addView(Button(this).apply{text="ADD / CHANGE LOGO";setOnClickListener{pickLogo()}})
        root!!.addView(tv("WATERMARK SETTINGS",19f))
        size=slider("Logo size (%)",2,50,18);opacity=slider("Logo opacity (%)",5,100,35)
        xpos=slider("Horizontal position (%)",0,100,50);ypos=slider("Vertical position (%)",0,100,50)
        rotation=slider("Rotation", -45,45,0)
        root!!.addView(tv("DISCLAIMER",19f))
        discOn=CheckBox(this).apply{text="Add disclaimer to every photo";isChecked=true}
        root!!.addView(discOn)
        discText=EditText(this).apply{setText("Disclaimer: Colours may vary slightly due to photographic lighting and effects.");setSingleLine(false)}
        root!!.addView(discText)
        discSize=slider("Disclaimer size",8,30,14);discOpacity=slider("Disclaimer opacity (%)",10,100,75)
        discOn.setOnCheckedChangeListener{_,v->discText.isEnabled=v;discSize.isEnabled=v;discOpacity.isEnabled=v;showPreview()}
        root!!.addView(tv("PHOTOS",19f))
        root!!.addView(Button(this).apply{text="SELECT PHOTOS";setOnClickListener{pickPhotos()}})
        root!!.addView(Button(this).apply{text="PREVIEW FIRST PHOTO";setOnClickListener{showPreview()}})
        preview=ImageView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,dp(300));scaleType=ImageView.ScaleType.FIT_CENTER}
        root!!.addView(preview)
        root!!.addView(tv("OUTPUT FORMAT",19f))
        format=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("JPG","PNG"))}
        root!!.addView(format)
        root!!.addView(Button(this).apply{text="PROCESS & SAVE ALL PHOTOS";setOnClickListener{process()}})
        status=tv("Ready",14f);root!!.addView(status)
    }
    private fun loadDefaultLogo(){logo=BitmapFactory.decodeResource(resources,resources.getIdentifier("nooras_logo","drawable",packageName));logoView.setImageBitmap(logo)}
    private fun pickLogo(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)},10)}
    private fun pickPhotos(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)},11)}
    override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return
        if(r==10)d.data?.let{try{logo=load(it);logoView.setImageBitmap(logo);showPreview()}catch(_:Exception){toast("Logo could not be loaded")}}
        if(r==11){photos.clear();d.clipData?.let{x->for(i in 0 until x.itemCount)photos.add(x.getItemAt(i).uri)}?:d.data?.let{photos.add(it)};status.text="${photos.size} photo(s) selected";showPreview()}
    }
    private fun load(u:Uri)=contentResolver.openInputStream(u)!!.use{BitmapFactory.decodeStream(it)!!}
    private fun showPreview(){val u=photos.firstOrNull()?:return;val l=logo?:return;pool.execute{try{val b=apply(load(u),l);runOnUiThread{preview.setImageBitmap(b)}}catch(_:Exception){}}}
    private fun apply(src:Bitmap,l:Bitmap):Bitmap{
        val base=src.copy(Bitmap.Config.ARGB_8888,true);val c=Canvas(base)
        var w=max(1,(base.width*(size.progress+2)/100f).toInt());var h=max(1,(l.height*w/l.width.toFloat()).toInt())
        var m=Bitmap.createScaledBitmap(l,w,h,true);val deg=rotation.progress-45;if(deg!=0)m=Bitmap.createBitmap(m,0,0,m.width,m.height,Matrix().apply{postRotate(deg.toFloat())},true)
        c.drawBitmap(m,base.width*(xpos.progress/100f)-m.width/2f,base.height*(ypos.progress/100f)-m.height/2f,Paint(3).apply{alpha=((opacity.progress+5)*2.55f).toInt()})
        if(discOn.isChecked)drawDisc(c,base.width,base.height)
        return base
    }
    private fun drawDisc(c:Canvas,w:Int,h:Int){
        val p=Paint(3).apply{textSize=max(20f,w*(discSize.progress+8)/1000f);color=Color.WHITE;textAlign=Paint.Align.CENTER;alpha=((discOpacity.progress+10)*2.55f).toInt()}
        val text=discText.text.toString().trim();if(text.isEmpty())return
        val lines=mutableListOf<String>();var line=""
        for(word in text.split(" ")){val t=if(line.isEmpty())word else "$line $word";if(p.measureText(t)<=w-40)line=t else{if(line.isNotEmpty())lines.add(line);line=word}};if(line.isNotEmpty())lines.add(line)
        val lh=p.textSize*1.25f;var y=h-18f-lh*(lines.size-1);for(x in lines){c.drawText(x,w/2f,y,p);y+=lh}
    }
    private fun process(){
        val l=logo?:return toast("Add a logo first");if(photos.isEmpty())return toast("Select photos first")
        status.text="Processing...";val fmt=format.selectedItem.toString();pool.execute{var n=0
            photos.forEachIndexed{i,u->try{val b=apply(load(u),l);save(b,"Nooras_Watermarked_${System.currentTimeMillis()}_$i",fmt);n++;runOnUiThread{status.text="Processing ${i+1}/${photos.size}"}}catch(_:Exception){}}
            runOnUiThread{status.text="Done! Saved $n/${photos.size} photo(s) in Pictures/Nooras Watermark"}
        }
    }
    private fun save(b:Bitmap,name:String,fmt:String){
        val png=fmt=="PNG";val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"$name.${if(png)"png" else "jpg"}");put(MediaStore.Images.Media.MIME_TYPE,if(png)"image/png" else "image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Nooras Watermark")}
        val u=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?:error("save failed")
        contentResolver.openOutputStream(u)!!.use{if(png)b.compress(Bitmap.CompressFormat.PNG,100,it)else b.compress(Bitmap.CompressFormat.JPEG,95,it)}
    }
    private fun toast(s:String)=runOnUiThread{Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}
    override fun onDestroy(){pool.shutdownNow();super.onDestroy()}
}
