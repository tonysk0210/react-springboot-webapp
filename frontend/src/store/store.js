import { configureStore } from "@reduxjs/toolkit";
import cartSliceReducer from "./cart-slice"; // 因為它是 default export，所以你可以自己命名成 cartReducer

/**
 * 建立「全域狀態中心」，真正保存目前的購物車資料
 * 
 * 把 cartSlice.reducer 這個 reducer function
 * 註冊到 Redux store 裡的 cart 這個 state key 底下
 * 
 * 所以最後 Redux state 會變成：
    state = {
      cart: [...]
    }
 * 
 */

// 1. 配置 Redux store 用於管理全局狀態
const store = configureStore({
  reducer: {
    cart: cartSliceReducer, // 將 cartSliceReducer 這個 reducer function 註冊到 Redux store 裡的 cart 這個 state key 底下（所以最後 Redux state 會變成：state = { cart: [...] })
  },
});

// 2. 當 store 中的狀態變化時，將購物車數據保存到 localStorage
store.subscribe(() => {
  try {
    // CART persistence
    const cart = store.getState().cart; // 1. 從 store 中獲取購物車數據
    localStorage.setItem("cart", JSON.stringify(cart)); // 2. 將購物車數據轉換成 JSON 字符串並存儲到 localStorage 中
  } catch (error) {
    console.error("無法將購物車數據保存到 localStorage:", error);
  }
  /**
   * store.subscribe(...) 有點像「Redux store 版的 effect listener」，但它不是 React 的 useEffect。
   * 只要 Redux store 發生任何 state 更新，就執行這個 callback。
   * 但它不是 React hook，沒有 dependency array，也不跟 component render 綁在一起。
   *
   * store.subscribe 會在每次 dispatch 導致 store 更新後執行 callback。
   * 這裡用它把最新的 state.cart 同步到 localStorage。
   */
});

export default store;
