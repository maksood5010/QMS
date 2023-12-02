package com.truelife.qms.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.truelife.qms.R
import com.truelife.qms.utils.QueueItem

class TokenAdapter(var queueList: ArrayList<QueueItem>) : RecyclerView.Adapter< TokenAdapter.RviewHolder>()  {

	val TAG = TokenAdapter::class.simpleName

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RviewHolder {
		if (viewType == 0){
			return RviewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_token_header_layout, parent, false))
		}else {
			return RviewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_token_layout, parent, false))
		}
	}

	override fun getItemViewType(position: Int): Int {
		return position
	}

	override fun getItemCount(): Int {
		return  queueList.size
	}
	fun submitList(queueList: List<QueueItem>){
//		this.queueList.clear()
//		this.queueList.addAll(queueList)
//		notifyItemRangeChanged(0,queueList.size)
//		this.queueList = queueList
		Log.d(TAG, "submitList: ${queueList.size}")
		notifyDataSetChanged()
	}
	@SuppressLint("ResourceType")
	override fun onBindViewHolder(holder: RviewHolder, position: Int) {
//
//		holder.token.text = "${position + 1}"
//		holder.room.text = "${position + 1}"
		if (position !=0) {
//			YoYo.with(Techniques.Flash).repeat(-1).duration(5000).playOn(holder.tokenBG)
//
//			YoYo.with(Techniques.Flash).repeat(-1).duration(5000).playOn(holder.roomBG)
			holder.bind(queueList[position])
		}
	}
	class RviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var token = itemView.findViewById<TextView>(R.id.token)
		var tokenBG = itemView.findViewById<View>(R.id.tokenBg)
		var room = itemView.findViewById<TextView>(R.id.room)
		var roomBG = itemView.findViewById<View>(R.id.roomBG)

		fun bind(queueItem: QueueItem){
			token.text = queueItem.token
			room.text = queueItem.room_number
		}
	}
}