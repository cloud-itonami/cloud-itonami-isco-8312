# physai-isco-8312 — 鉄道の制動・信号・転てつ作業員（ISCO 8312）の保守を支えるロボットの physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isco-8312`、ISCO 8312 鉄道の制動手・信号手・転てつ手）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 鉄道運行の段取り・記録ロボットが、運行記録の記録、乗務員の交番調整、保守発注の起案を行う（転てつ器の転換・信号の現示・進路の確定は決してしない）。
その物理的な仕事（線路閉鎖の間に信号・転てつ機の予備品を積んだ軌道トロリーを勾配区間で走らせることと、断熱された沿線の機器箱の中の継電器に届く熱）を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:spares-trolley-on-grade` | transport | 動力付き軌道トロリーが予備品を積んで 1° の勾配を 400 m 走る（駆動力 700 N、最高 3 m/s） | 1 区間の所要時間 | 180 s（estimate） |
| `:location-case-relays` | thermal | 断熱（25 mm 発泡材）した沿線機器箱が午後の日射に 4 時間さらされる（外面 = 相当外気温度、内面 = 継電器ラックの空気） | 内面の最高温度 | 45 °C（estimate） |

測定の入口: `kbb -M:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:physai-test`（`test-physai/railsignalcrew/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する。repo 自身の `test/` の .cljk も同じ runner で走り、計 23 test / 54 assertion）。

## 測って分かったこと・限界（成長の第一候補）

1. **トロリー**: 積荷 500 kg までは 138.96 s で変わらない（加速度上限 0.4 m/s² と最高速度が効く）。1000 kg から駆動力が効き（drive-limited? true）、1500 kg で 142.73 s、
   2500 kg で 163.0 s。限界 180 s を超える積荷は **2740.8 kg**。エネルギーは 29.3 kJ → 229.9 kJ で大半は勾配の位置エネルギー。
2. **機器箱**: 25 mm の発泡断熱では内面温度は約 2 時間で定常に達し、相当外気温度 40 °C で 31.6 °C、85 °C でも 38.8 °C（15 °C あたり 2.4 °C 増）。
   掃引範囲では限界 45 °C に届かず、境界は置いていない。実際に効くのは箱の中の継電器・電源の発熱（この平板モデルには入らない）で、次の反復で内部発熱を扱う case を足す候補。
3. **estimate のままの値**: 線路閉鎖内の区間許容 180 s（作業計画の実時間で置き換える）、トロリーの駆動力 700 N・転がり抵抗係数 0.003（トロリーの仕様書で置き換える）、
   内面上限 45 °C（継電器の使用温度範囲の仕様で置き換える）、発泡材の熱伝導率 0.025 W/mK・外面の熱伝達係数 20 W/m²K・相当外気温度の範囲。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isco-8312 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:physai-test → kbb -M:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isco-8312 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
