import {categories,topics,sources,newsTypes,unavailableSources} from '../shared/catalog.js';
import {initialData} from '../shared/state.js';
import {mkdir,writeFile} from 'node:fs/promises';
await mkdir('android/app/src/main/assets',{recursive:true});
await writeFile('android/app/src/main/assets/fallow-catalog.json',JSON.stringify({categories,topics,sources,newsTypes,unavailableSources,defaults:initialData().settings},null,2));
