import { createContext, useEffect, useContext, useReducer } from "react";

// 1. 定義 AuthContext 和 useAuth custom hook；被 AuthProvider 包住的元件可以透過 useAuth 取得登入狀態和操作方法
const AuthContext = createContext();
export const useAuth = () => useContext(AuthContext);

const LOGIN_SUCCESS = "LOGIN_SUCCESS";
const LOGOUT = "LOGOUT";

// 這邊使用立即執行函數來初始化 authState 狀態，因此重新整理頁面後，登入狀態可以被恢復。
const initialAuthState = (() => {
  try {
    const jwtToken = localStorage.getItem("jwtToken"); // 取得 localStorage 中的 jwtToken
    const user = localStorage.getItem("user"); // 取得 localStorage 中的 user
    if (jwtToken && user) {
      // 如果 jwtToken 和 user 都存在 (代表已登入)，則返回一個包含這些信息的 authState 對象，並且設置 isAuthenticated 為 true
      return {
        jwtToken: jwtToken, // 設置 jwtToken
        user: JSON.parse(user), // 將字符串轉換為 JavaScript object
        isAuthenticated: true, // 設置為已登入狀態
      };
    }
  } catch (error) {
    console.error("無法取得 localStorage 中的登入數據:", error);
  }

  // 否則返回一個表示未登入狀態的 (代表未登入) authState 對象，jwtToken 和 user 都設置為 null，isAuthenticated 設置為 false
  return {
    jwtToken: null,
    user: null,
    isAuthenticated: false,
  };
})(); // () 代表「檔案被載入時立刻執行一次」；所以只要 auth-context.jsx 被 import，例如 main.jsx import AuthProvider，這段初始化邏輯就會跑。

// 2. authReducer 用來決定「登入狀態 authState 要怎麼更新」的函式。
const authReducer = (currentState, action) => {
  switch (action.type) {
    case LOGIN_SUCCESS:
      return {
        jwtToken: action.payload.jwtToken,
        user: action.payload.user,
        isAuthenticated: true,
      };
    case LOGOUT:
      return {
        jwtToken: null,
        user: null,
        isAuthenticated: false,
      };
    default:
      return currentState;
  }
};

// 3. 定義 AuthProvider 組件
export const AuthProvider = ({ children }) => {
  // 3.1 使用 useReducer 來管理 auth 狀態：
  //   authState → 目前登入狀態
  //   dispatch  → 發送狀態更新要求
  // authReducer → 決定新的狀態內容；authReducer 執行 dispatch 後返回的狀態
  const [authState, dispatch] = useReducer(authReducer, initialAuthState);

  // 3.2 監聽 authState 變化，並將其保存或從 localStorage 中移除
  useEffect(() => {
    try {
      // 只有在用戶登入時才將 jwtToken 和 user 保存到 localStorage
      if (authState.isAuthenticated) {
        localStorage.setItem("jwtToken", authState.jwtToken);
        localStorage.setItem("user", JSON.stringify(authState.user));
      }
    } catch (error) {
      console.error("無法將登入狀態保存到 localStorage:", error);
    }
  }, [authState]);

  // 這是登入成功時的 action creator - 用於更新 auth 狀態
  const loginSuccess = (jwtToken, user) => {
    dispatch({ type: LOGIN_SUCCESS, payload: { jwtToken, user } }); // 由 Login.jsx 呼叫 /auth/login api，並帶入後端返回的 jwtToken 和 user 來更新前端 context
    // payload 是 action 的載荷，用來傳遞數據
  };

  // 這是登出時的 action creator - 用於更新 auth 狀態，並清除 localStorage 和 sessionStorage 中的相關數據
  const logout = () => {
    localStorage.removeItem("jwtToken");
    localStorage.removeItem("user");
    sessionStorage.removeItem("redirectPath");
    sessionStorage.removeItem("skipRedirectPath");
    dispatch({ type: LOGOUT });
  };

  return (
    // AuthContext.Provider 的 value prop 是一個物件，裡面包含了當前的 jwtToken、user、isAuthenticated 狀態，以及 loginSuccess 和 logout 這兩個函式，這樣被 AuthProvider 包住的組件就可以通過 useAuth() 來訪問這些值和函式了。
    <AuthContext.Provider
      value={{
        jwtToken: authState.jwtToken,
        user: authState.user,
        isAuthenticated: authState.isAuthenticated,
        loginSuccess, // 登入成功時的 action creator
        logout, // 登出時的 action creator
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
