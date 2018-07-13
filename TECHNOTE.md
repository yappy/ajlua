# Technical Note

## エラーハンドリング

### Lua longjmp, C++ exception
* Lua はエラー発生時に longjmp() を行う。
  * Lua C API が longjmp() する可能性があるかどうかは
    Lua Reference Manual の各 API の右上を参照。
* ジャンプ先の setjmp() の設定は pcall の地点で行われる。
* 保護されていない (longjmp() 先が設定されていない) 状態で Lua エラーが起きると
  lua_atpanic() で設定されたパニックハンドラが呼ばれる。これはロジックエラー。
  パニックハンドラが返るとその後 abort() が呼ばれてしまうので何か適切な処理を
  行う必要がある。JniEnv::FatalError() が適当か。
* Lua を C++ コンパイルした場合、C++ 例外 (throw, catch) によるエラー処理に変化する。

* C++ スタックが longjmp() で消し飛ばされるとデストラクタが呼ばれない。
  * デストラクタを持つ C++ オブジェクトが生存しているスコープ内でエラーが発生する
    可能性のある Lua C API を呼んではいけない。
  * ただし Lua を C++ コンパイルして longjmp() の代わりに C++ 例外を
    使うようにすれば問題ない。
  * Android はデフォルトでは例外機能が無効になっているので注意。
    もちろん例外発生時のデストラクタ呼び出しコード分のオーバーヘッドは増える。

* Java スタックが longjmp() で消し飛ばされると native call の後処理も
  何もかもが吹き飛び、非常にまずい。
  * そもそも (extern "C" の) DLL 関数がその外へ longjmp() や throw をするのは駄目。

### Java exception
* Java 例外発生状態ではごく一部の限られた JNI API 以外は呼んではいけない。
* Oracle VM (or OpenJDK) では呼んではいけないものを呼んでも何も起きないようだが、
  Android (DALVIK? ART?) では VM が落ちる。
  したがって確認には Android 実機 or エミュレータ上でのテストが有効。
* ネイティブコード中で Java 例外を発生させた後、Lua error を発生 (longjmp) させれば
  pcall ポイントに飛べるが、pcall は Lua からも呼び出すことができる。
  ここでジャンプが止まってしまうと Java 例外状態で Lua コードが実行続行できてしまう。
  例外発生後に一旦グローバル参照を作って管理クラスのフィールドにセットして
  あとでリスローすれば例外発生状態での JNI API 呼び出しは防げるが、
  例外をサスペンドしたまま Lua コードが走ってしまうのは変わらない。
  そこから JNI を叩く C++ コードを呼び出してしまうとまずい。
  (全 lua_cfunction の先頭で例外チェックする必要が出てくる)
  * pcall を削除する。
  * pcall を自作関数で置き換える。
    * オリジナルの pcall 後に例外状態なら即 longjmp() する。

### JNI native call での方針
* longjmp() する可能性のある　Lua C API を呼んではいけない。
  * 飛び先がないので longjmp() する前に panic する。
  * 危険な Lua C API を呼ぶ処理は C(C++) 関数を push して lua_pcall() する。
  * その中ではデストラクタは使ってはいけない。(Lua を C++ compile した場合は可)
* C++ 例外を投げてはいけない。

### Java -> C++ -> Lua -> C++ -> Java call での方針
* Java method で例外発生した場合、即座に呼び出し元の Java コードに処理を戻したい。
* Lua エラーを起こせば pcall ポイントまでジャンプできるが、pcall は Lua からも
  呼び出すことができてしまう。
  * Lua standard library から pcall を削除する。
  * pcall を改造版に置き換える。
    1. オリジナル pcall を呼ぶ。
    2. pcall が返った後 Java 例外チェックし、発生していたら Lua error を発生させる。
