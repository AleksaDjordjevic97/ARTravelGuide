package com.aleksadjordjevic.augmentedtravelguide.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aleksadjordjevic.augmentedtravelguide.R
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.bumptech.glide.Glide

class GuidePlaceAdapter(private val context: Context,
                        private val guidePlaceListener:OnGuidePlaceListener,
                        private val placesList:ArrayList<Place>): RecyclerView.Adapter<GuidePlaceAdapter.GuidePlaceViewHolder>()

{
    interface OnGuidePlaceListener
    {
        fun onEditCellClick(position: Int)
        fun onRemoveCellClick(position: Int)
    }

    inner class GuidePlaceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    {
        val guidePlacesCellImage = itemView.findViewById<ImageView>(R.id.guidePlacesCellImage)
        val guidePlacesCellName = itemView.findViewById<TextView>(R.id.guidePlacesCellName)
        val guidePlacesCellDescription = itemView.findViewById<TextView>(R.id.guidePlacesCellDescription)
        val btnGuidePlacesEdit = itemView.findViewById<ImageButton>(R.id.btnGuidePlacesEdit)
        val btnGuidePlacesRemove = itemView.findViewById<ImageButton>(R.id.btnGuidePlacesRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuidePlaceViewHolder
    {
        val view = LayoutInflater.from(context).inflate(R.layout.view_holder_guide_places,parent,false)
        return GuidePlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuidePlaceViewHolder, position: Int)
    {
        val place = placesList[position]

        Glide.with(context).load(place.image_for_scanning).into(holder.guidePlacesCellImage)
        holder.guidePlacesCellName.text = place.name
        holder.guidePlacesCellDescription.text = place.description

        holder.btnGuidePlacesEdit.setOnClickListener { guidePlaceListener.onEditCellClick(position) }
        holder.btnGuidePlacesRemove.setOnClickListener { guidePlaceListener.onRemoveCellClick(position) }
    }

    override fun getItemCount(): Int
    {
        return placesList.size
    }
}