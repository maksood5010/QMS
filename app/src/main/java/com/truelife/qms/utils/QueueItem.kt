package com.truelife.qms.utils

data class QueueItem(
	val ar_label: String,
	val ar_name: String,
	val en_label: String,
	val en_name: String,
	val room_name: String,
	val room_number: String,
	val time: String,
	val token: String
){

}


data class QueueLine(
	val QueueLine: List<QueueItem>
)
