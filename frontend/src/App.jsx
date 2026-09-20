import Header from "./components/Header";
import Footer from "./components/footer/Footer";
import { Outlet, useNavigation } from "react-router-dom";
// 可以用來判斷當前是否正在進行導航，例如：正在加載新頁面、正在提交表單等等，從而可以在 UI 上顯示相應的加載指示器或者禁用某些按鈕等等。

function App() {
  const navigation = useNavigation();
  // 使用 useNavigation hook 用來取得目前路由導航的狀態，主要是用來顯示 loading 狀態; navigation.state // "idle" | "loading" | "submitting"
  // useNavigation 對這整個 RouterProvider 裡的 route navigation 都會奏效，含父 route 和所有 child routes

  return (
    <>
      <Header />
      {/* 根據導航狀態來決定是否顯示加載 */}
      {navigation.state === "loading" ? (
        <div className="flex items-center justify-center min-h-213">
          <span className="text-4xl font-semibold text-brand dark:text-light">
            Loading...
          </span>
        </div>
      ) : (
        <Outlet /> // Outlet 是「父路由放子路由畫面的位置」。
      )}
      <Footer />
    </>
  );
}

export default App;
