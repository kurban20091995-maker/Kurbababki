package com.milibaby.app

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.max

class ChartView(context:Context):View(context){ var values:List<Pair<String,Int>> = emptyList(); var accent:Int=Color.rgb(215,105,140)
 override fun onDraw(c:Canvas){ super.onDraw(c); if(values.isEmpty()){ val p=Paint(1).apply{color=Color.GRAY;textSize=34f}; c.drawText("Пока недостаточно данных",30f,height/2f,p); return}; val maxV=max(1,values.maxOf{it.second}); val pad=36f; val base=height-56f; val gap=(width-pad*2)/values.size.toFloat(); val p=Paint(1).apply{color=accent}; values.forEachIndexed{i,v-> val h=(base-30f)*(v.second/maxV.toFloat()); val left=pad+i*gap+gap*.18f; c.drawRoundRect(left,base-h,left+gap*.64f,base,16f,16f,p) }; val t=Paint(1).apply{color=Color.DKGRAY;textSize=22f;textAlign=Paint.Align.CENTER}; values.forEachIndexed{i,v-> if(values.size<=10||i%max(1,values.size/7)==0)c.drawText(v.first,pad+i*gap+gap*.5f,height-18f,t)} }
}
