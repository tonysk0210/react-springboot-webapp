import { createSlice } from "@reduxjs/toolkit";

/**
cart-slice.js：定義「購物車有哪些資料、可以怎麼修改」
定義 cart 的初始值、可用 actions、更新規則

store.js
把 cartSlice.reducer 掛到 Redux store 的 state.cart 上

React component
用 useSelector(state => state.cart) 讀資料
用 dispatch(addToCart(...)) 觸發 cartSlice 裡的 reducer
Redux 明確用 useSelector 讀、useDispatch 寫；

目前流程確實是：
cart-slice.js 定義 cart slice
↓
export default cartSlice.reducer
↓
store.js import 成 cartReducer
↓
configureStore({ reducer: { cart: cartReducer } })
↓
React component 用 useSelector / useDispatch 操作 state.cart

createSlice 會根據 reducers 裡的 addToCart reducer，
自動產生一個同名的 addToCart action creator。
 */

const initialCart = JSON.parse(localStorage.getItem("cart")) || []; // 從 localStorage 中讀取購物車數據並解析成 JavaScript 對象

// 1. 「定義購物車規則」創建一個 Redux slice 來管理購物車狀態，需要指定 name、initialState 和 reducers
const cartSlice = createSlice({
  name: "cart", // slice 的名稱 用於 debug : cart/addToCart, cart/removeFromCart, cart/clearCart
  initialState: initialCart, // 初始狀態
  // reducers 是一個對象，包含多個 reducer 函數
  reducers: {
    // reducers 裡的 addToCart 是 case reducer，負責處理 cart state 的更新邏輯。
    // createSlice 會根據它自動產生同名的 action creator： addToCart(payload) => ({ type: "cart/addToCart", payload })
    addToCart(currentState, action) {
      const { product, quantity } = action.payload;

      const existingItem = currentState.find((item) => item.id === product.id);

      if (existingItem) {
        // Redux Toolkit createSlice: 可以寫「像是直接修改 state」的程式碼，因為 Immer 會幫你轉成 immutable update。
        existingItem.quantity += quantity;
        /*
         * 在 Context reducer 裡要寫成：
          return currentState.map(
            (item) =>
              item.id === product.id
                ? { ...item, quantity: item.quantity + quantity } // 現有的產品的數量增加傳入的數量
                : item, // 若不是目標商品的其他商品，就不要改它
          );
         */
      } else {
        currentState.push({ ...product, quantity }); // 若購物車中沒有此產品，則將產品添加到購物車中 array.push(...) 是 JavaScript 陣列的語法。它的意思是：把一個或多個元素加到陣列最後面。
        /*
         * 在 Context reducer 裡要寫成：
          return [...currentState, { ...product, quantity }]; // 商品不存在於購物車，就將它添加到購物車中
         */
      }
    },

    // removeFromCart reducer 函數
    removeFromCart(currentState, action) {
      const { id } = action.payload;
      return currentState.filter((item) => item.id !== id);
    },

    // clearCart reducer 函數
    clearCart() {
      return [];
    },
  },
});

// 2. 導出 cartSlice 的 actions 和 reducer
export const { addToCart, removeFromCart, clearCart } = cartSlice.actions;
export default cartSlice.reducer;

// 3. 導出 selectors：這裡的 state 是 Redux store 目前的完整 state，是 useSelector(...) 呼叫 selector 時自動傳進來的。

// 提供對購物車數據的訪問：state = Redux store 目前的完整狀態
export const selectCartItems = (state) => state.cart; // 從 Redux 狀態中選擇購物車數據

// 提供對購物車總數量的訪問
export const selectTotalQuantity = (state) =>
  // 檢查 state.cart 是否為陣列，如果是則計算總數量，否則返回 0
  Array.isArray(state.cart)
    ? state.cart.reduce((acc, item) => acc + item.quantity, 0)
    : 0;

// 提供對購物車總價格的訪問
export const selectTotalPrice = (state) =>
  // 檢查 state.cart 是否為陣列，如果是則計算總價格，否則返回 0
  Array.isArray(state.cart)
    ? state.cart.reduce((acc, item) => acc + item.quantity * item.price, 0)
    : 0;
