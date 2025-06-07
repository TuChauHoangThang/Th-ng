package com.example.weatherapp.ui.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import com.example.weatherapp.model.ForecastItem;
import java.util.List;

public class ForecastDiffCallback extends DiffUtil.Callback {

    private final List<ForecastItem> oldList;
    private final List<ForecastItem> newList;

    public ForecastDiffCallback(List<ForecastItem> oldList, List<ForecastItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // So sánh định danh duy nhất của item, ở đây là dt (timestamp)
        return oldList.get(oldItemPosition).getDt() == newList.get(newItemPosition).getDt();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // So sánh nội dung của item
        // Trong trường hợp này, nếu timestamp giống nhau, ta coi như nội dung cũng giống nhau
        // để đơn giản. Bạn có thể thêm so sánh chi tiết hơn nếu cần.
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
} 