package project.unibo.tankyou.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.utils.Constants
import java.text.SimpleDateFormat
import java.util.Locale

class GasStationDetailCard(
    private val context: Context,
    private val parentView: RelativeLayout,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    private var cardView: LinearLayout? = null
    private var fuelAdapter: FuelPriceAdapter? = null
    private var isVisible = false
    private val screenHeight = getScreenHeight()
    private val cardHeight = (screenHeight * 0.65).toInt()

    private val fuelTypeNames = arrayOf("", "Benzina", "Diesel", "Metano", "GPL")
    private val flagNames =
        arrayOf("", "Agip Eni", "Api-Ip", "Esso", "Pompe Bianche", "Q8", "Tamoil")

    fun showStationDetails(gasStation: GasStation) {
        if (cardView == null) {
            createCard()
        }

        populateStationInfo(gasStation)
        loadFuelPrices(gasStation.id)

        if (!isVisible) {
            showCard()
        }
    }

    fun hide() {
        if (isVisible) {
            hideCard()
        }
    }

    private fun getScreenHeight(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun createCard() {
        cardView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            background = createCardBackground()
            elevation = 100f

            val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                cardHeight
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }

            this.layoutParams = layoutParams
            visibility = View.GONE
            translationY = cardHeight.toFloat()
        }

        createDragHandle()
        createScrollableContent()

        parentView.addView(cardView)
    }

    private fun createDragHandle() {
        val handleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 24, 0, 16)
        }

        val handle = View(context).apply {
            background = createHandleBackground()
            layoutParams = LinearLayout.LayoutParams(120, 12)
        }

        handleContainer.addView(handle)
        cardView?.addView(handleContainer)
    }

    private fun createScrollableContent() {
        val scrollContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 0, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        createHeader(scrollContainer)
        createStationInfo(scrollContainer)
        createFuelPricesSection(scrollContainer)

        cardView?.addView(scrollContainer)
    }

    private fun createHeader(parent: LinearLayout) {
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 24)
        }

        val titleText = TextView(context).apply {
            text = "Gas Station Details"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A1A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = TextView(context).apply {
            text = "✕"
            textSize = 20f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            background = createRippleBackground()
            setOnClickListener { hide() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)
        parent.addView(headerLayout)
    }

    private fun createStationInfo(parent: LinearLayout) {
        val infoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = createInfoBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        infoContainer.addView(createStationNameText())
        infoContainer.addView(createStationAddressText())
        infoContainer.addView(createStationProvinceText())

        val detailsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }

        detailsContainer.addView(createInfoRow("Owner: ", createStationOwnerText()))
        detailsContainer.addView(createInfoRow("Brand: ", createStationFlagText()))

        infoContainer.addView(detailsContainer)
        parent.addView(infoContainer)
    }

    private fun createFuelPricesSection(parent: LinearLayout) {
        val titleText = TextView(context).apply {
            text = "Fuel Prices"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 16)
        }

        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isNestedScrollingEnabled = false
        }

        fuelAdapter = FuelPriceAdapter()
        recyclerView.adapter = fuelAdapter

        parent.addView(titleText)
        parent.addView(recyclerView)
    }

    private fun createStationNameText() = TextView(context).apply {
        textSize = 20f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(Color.parseColor("#1A1A1A"))
        tag = "stationName"
    }

    private fun createStationAddressText() = TextView(context).apply {
        textSize = 16f
        setTextColor(Color.parseColor("#666666"))
        setPadding(0, 8, 0, 0)
        tag = "stationAddress"
    }

    private fun createStationProvinceText() = TextView(context).apply {
        textSize = 14f
        setTextColor(Color.parseColor("#999999"))
        setPadding(0, 4, 0, 0)
        tag = "stationProvince"
    }

    private fun createStationOwnerText() = TextView(context).apply {
        textSize = 14f
        setTextColor(Color.parseColor("#666666"))
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tag = "stationOwner"
    }

    private fun createStationFlagText() = TextView(context).apply {
        textSize = 14f
        setTextColor(Color.parseColor("#666666"))
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tag = "stationFlag"
    }

    private fun createInfoRow(label: String, valueView: TextView): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)

            val labelText = TextView(context).apply {
                text = label
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                minWidth = 200
            }

            addView(labelText)
            addView(valueView)
        }
    }

    private fun createCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadii = floatArrayOf(32f, 32f, 32f, 32f, 0f, 0f, 0f, 0f)
            setStroke(1, Color.parseColor("#E0E0E0"))
        }
    }

    private fun createHandleBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#CCCCCC"))
            cornerRadius = 6f
        }
    }

    private fun createInfoBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F8F9FA"))
            cornerRadius = 16f
            setStroke(1, Color.parseColor("#E9ECEF"))
        }
    }

    private fun createRippleBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#10000000"))
            cornerRadius = 24f
        }
    }

    private fun populateStationInfo(gasStation: GasStation) {
        cardView?.findViewWithTag<TextView>("stationName")?.text = gasStation.name ?: "Gas Station"
        cardView?.findViewWithTag<TextView>("stationAddress")?.text =
            listOfNotNull(gasStation.address, gasStation.city).joinToString(", ")
                .ifEmpty { "Address not available" }
        cardView?.findViewWithTag<TextView>("stationProvince")?.text = gasStation.province ?: ""
        cardView?.findViewWithTag<TextView>("stationOwner")?.text =
            gasStation.owner ?: "Not specified"

        val flagName = if (gasStation.flag > 0 && gasStation.flag < flagNames.size) {
            flagNames[gasStation.flag]
        } else {
            "Independent"
        }
        cardView?.findViewWithTag<TextView>("stationFlag")?.text = flagName
    }

    private fun loadFuelPrices(stationId: Long) {
        lifecycleScope.launch {
            try {
                val fuelPrices = Constants.App.REPOSITORY.getFuelPricesForStation(stationId)
                fuelAdapter?.updateFuelPrices(fuelPrices)
            } catch (e: Exception) {
                e.printStackTrace()
                fuelAdapter?.updateFuelPrices(emptyList())
            }
        }
    }

    private fun showCard() {
        cardView?.let { card ->
            card.visibility = View.VISIBLE

            card.animate()
                .translationY(0f)
                .setDuration(350)
                .withStartAction {
                    isVisible = true
                }
                .start()
        }
    }

    private fun hideCard() {
        cardView?.let { card ->
            card.animate()
                .translationY(cardHeight.toFloat())
                .setDuration(300)
                .withEndAction {
                    card.visibility = View.GONE
                    isVisible = false
                }
                .start()
        }
    }

    private inner class FuelPriceAdapter :
        RecyclerView.Adapter<FuelPriceAdapter.FuelPriceViewHolder>() {
        private var fuelPrices: List<Fuel> = emptyList()

        fun updateFuelPrices(newFuelPrices: List<Fuel>) {
            fuelPrices = newFuelPrices
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuelPriceViewHolder {
            val itemView = createFuelItemView()
            return FuelPriceViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: FuelPriceViewHolder, position: Int) {
            holder.bind(fuelPrices[position])
        }

        override fun getItemCount(): Int = fuelPrices.size

        private fun createFuelItemView(): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(20, 16, 20, 16)
                background = createFuelItemBackground()
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 12)
                }

                val leftColumn = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                    addView(TextView(context).apply {
                        textSize = 16f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(Color.parseColor("#1A1A1A"))
                        tag = "fuelType"
                    })

                    addView(TextView(context).apply {
                        textSize = 13f
                        setTextColor(Color.parseColor("#666666"))
                        setPadding(0, 4, 0, 0)
                        tag = "fuelSelf"
                    })
                }

                val rightColumn = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END

                    addView(TextView(context).apply {
                        textSize = 18f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(Color.parseColor("#2196F3"))
                        gravity = Gravity.END
                        tag = "fuelPrice"
                    })

                    addView(TextView(context).apply {
                        textSize = 11f
                        setTextColor(Color.parseColor("#999999"))
                        setPadding(0, 4, 0, 0)
                        gravity = Gravity.END
                        tag = "fuelDate"
                    })
                }

                addView(leftColumn)
                addView(rightColumn)
            }
        }

        private fun createFuelItemBackground(): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 12f
                setStroke(1, Color.parseColor("#E9ECEF"))
            }
        }

        inner class FuelPriceViewHolder(private val itemView: LinearLayout) :
            RecyclerView.ViewHolder(itemView) {
            fun bind(fuel: Fuel) {
                val fuelTypeName = if (fuel.type > 0 && fuel.type < fuelTypeNames.size) {
                    fuelTypeNames[fuel.type]
                } else {
                    "Unknown Fuel"
                }

                itemView.findViewWithTag<TextView>("fuelType")?.text = fuelTypeName
                itemView.findViewWithTag<TextView>("fuelPrice")?.text =
                    String.format("€%.3f", fuel.price)
                itemView.findViewWithTag<TextView>("fuelSelf")?.text =
                    if (fuel.self) "Self Service" else "Full Service"

                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val date = inputFormat.parse(fuel.date)
                    itemView.findViewWithTag<TextView>("fuelDate")?.text =
                        "Updated: ${date?.let { outputFormat.format(it) } ?: fuel.date}"
                } catch (e: Exception) {
                    itemView.findViewWithTag<TextView>("fuelDate")?.text = fuel.date
                }
            }
        }
    }
}