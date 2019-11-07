/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Apr 29, 2019
 *
 */

package com.odysseusinc.arachne.datanode.util;

import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AtlasUtils {
	private AtlasUtils() {}

	public static List<Atlas> filterAtlasByVersion27(List<Atlas> atlasList) {

		return atlasList.stream()
						.filter(a -> Objects.nonNull(a) && Objects.nonNull(a.getVersion()))
						.filter(atlas -> Constants.Atlas.ATLAS_2_7_VERSION.isLesserOrEqualsThan(atlas.getVersion()))
						.collect(Collectors.toList());
	}
}
