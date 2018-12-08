package boonleng94.iguide.Controller

//Class to filter the RSSI values and do data fitting
class RSSIFilter {
    private fun getMean(values: ArrayList<Int>): Double {
        var sum = 0
        for (value in values) {
            sum += value
        }

        return (sum / values.size).toDouble()
    }

    //Returns the variance of the list of RSSI values
    private fun getVariance(values: ArrayList<Int>): Double {
        val mean = getMean(values)
        var temp = 0

        for (a in values) {
            temp += ((a - mean) * (a - mean)).toInt()
        }

        return (temp / (values.size - 1)).toDouble()
    }

    //Returns the standard deviation of the list of RSSI values
    private fun getStdDev(values: ArrayList<Int>): Double {
        return Math.sqrt(getVariance(values))
    }

    //Returns the list of RSSI values with outliers removed
    fun eliminateOutliers(values: ArrayList<Int>, scaleOfElimination: Float): ArrayList<Int> {
        val mean = getMean(values)
        val stdDev = getStdDev(values)

        val newList = ArrayList<Int>()

        for (value in values) {
            val isLessThanLowerBound = value < mean - stdDev * scaleOfElimination
            val isGreaterThanUpperBound = value > mean + stdDev * scaleOfElimination
            val isOutOfBounds = isLessThanLowerBound || isGreaterThanUpperBound

            if (!isOutOfBounds) {
                newList.add(value)
            }
        }

        val countOfOutliers = values.size - newList.size

        return if (countOfOutliers == 0) {
            values
        } else eliminateOutliers(newList, scaleOfElimination)
    }

    //Returns the mode of the list of RSSI values
    fun getMode(values: ArrayList<Int>): Int {
        var maxValue = values[0]
        var maxCount = 0

        for ((i) in values.withIndex()) {
            var count = 0

            for ((j) in values.withIndex()) {
                if (values[j] == values[i]) {
                    count++
                }
            }

            if (count > maxCount) {
                maxCount = count
                maxValue = values[i]
            }
        }

        return maxValue
    }
}